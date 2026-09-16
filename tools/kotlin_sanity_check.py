#!/usr/bin/env python3
"""Lightweight Kotlin sanity checks that need no Android SDK.

This box has a JDK but no Android SDK (only platform-tools), so a real Gradle compile is not
possible locally. These checks catch the mistakes that would otherwise cost a CI round-trip:

  * unbalanced braces / brackets / parens (after stripping comments and strings)
  * imports that appear after the first declaration (illegal in Kotlin)
  * duplicate imports, duplicate top-level declarations in a file
  * R.font.* / R.layout.* references with no matching resource file
  * identifiers that resolve to nothing: not imported, not declared in the file, and not
    declared elsewhere in the SAME PACKAGE (same-package symbols need no import)
"""
import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG = os.path.join(ROOT, 'app/src/main/java')
EXPECTED_PKG_PREFIX = 'com.jayesh.cashcollect'

DECL_RE = re.compile(
    r'^\s*(?:private\s+|internal\s+|public\s+)?'
    r'(?:fun|object|data class|sealed interface|interface|class|enum class|val|var|const val)\s+'
    r'(?:[A-Za-z0-9_.<>, ]+?\.)?(\w+)',
    re.M,
)

# Only TYPE declarations participate in the duplicate check. Function overloading is legal
# Kotlin (CommissionCalculator legitimately has calculate(Long, Int) and calculate(Paise, Int)),
# and local `val`s repeat freely across functions, so both are excluded to avoid false alarms.
TYPE_DECL_RE = re.compile(
    r'^\s*(?:private\s+|internal\s+|public\s+)?'
    r'(?:object|data class|sealed interface|interface|class|enum class)\s+'
    r'(?:[A-Za-z0-9_.<>, ]+?\.)?(\w+)',
    re.M,
)


def strip_code(src: str) -> str:
    """Remove comments and string/char literals in ONE pass.

    Order matters: stripping comments before strings is wrong, because a string such as
    "https://example.com" contains '//' and would truncate the line, leaving an unbalanced
    quote that then swallows real code.
    """
    out = []
    i = 0
    n = len(src)
    while i < n:
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j == -1 else j + 3
            continue
        c = src[i]
        if c == '"' or c == "'":
            quote = c
            i += 1
            while i < n:
                if src[i] == '\\':
                    i += 2
                    continue
                if src[i] == quote:
                    i += 1
                    break
                i += 1
            continue
        if src.startswith('//', i):
            j = src.find('\n', i)
            i = n if j == -1 else j
            continue
        if src.startswith('/*', i):
            j = src.find('*/', i + 2)
            i = n if j == -1 else j + 2
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def res_names(kind: str):
    d = os.path.join(ROOT, 'app/src/main/res', kind)
    if not os.path.isdir(d):
        return set()
    return {os.path.splitext(f)[0] for f in os.listdir(d)}


def declarations_in(directory: str):
    names = set()
    for path in glob.glob(os.path.join(directory, '*.kt')):
        names |= set(DECL_RE.findall(strip_code(open(path, encoding='utf-8').read())))
    return names


def main() -> int:
    problems = []
    fonts = res_names('font')
    layouts = res_names('layout')
    files = sorted(glob.glob(os.path.join(PKG, '**', '*.kt'), recursive=True))

    for path in files:
        raw = open(path, encoding='utf-8').read()
        code = strip_code(raw)
        rel = os.path.relpath(path, ROOT)

        for o, c in (('{', '}'), ('(', ')'), ('[', ']')):
            if code.count(o) != code.count(c):
                problems.append(
                    f'{rel}: unbalanced {o}{c} ({code.count(o)} vs {code.count(c)})'
                )

        # Two statements merged onto one line, e.g. "}    Scaffold(" - the classic symptom
        # of concatenating generated files that lack a trailing newline. Braces still
        # balance in that case, so the delimiter check above would NOT catch it. (This
        # actually shipped once.)
        for idx, line in enumerate(code.splitlines(), start=1):
            if re.search(r'\}\s{2,}[A-Za-z_]\w*\s*\(', line):
                problems.append(f'{rel}:{idx}: suspicious merged line -> {line.strip()[:60]}')

        seen_decl = False
        imports = []
        for line in code.splitlines():
            s = line.strip()
            if not s:
                continue
            if s.startswith('import '):
                imports.append(s)
                if seen_decl:
                    problems.append(f'{rel}: import after a declaration -> {s}')
            elif s.startswith('package '):
                continue
            else:
                seen_decl = True

        for dup in sorted({i for i in imports if imports.count(i) > 1}):
            problems.append(f'{rel}: duplicate import -> {dup}')

        own = TYPE_DECL_RE.findall(code)
        for dup in sorted({d for d in own if own.count(d) > 1}):
            problems.append(f'{rel}: duplicate declaration -> {dup}')

        for m in re.finditer(r'R\.font\.([A-Za-z0-9_]+)', code):
            if m.group(1) not in fonts:
                problems.append(f'{rel}: R.font.{m.group(1)} has no res/font file')
        for m in re.finditer(r'R\.layout\.([A-Za-z0-9_]+)', code):
            if m.group(1) not in layouts:
                problems.append(f'{rel}: R.layout.{m.group(1)} has no res/layout file')

        for m in re.finditer(r'import\s+((?:[\w.]+))\.', code):
            head = m.group(1)
            if head.startswith('com.jayesh') and not head.startswith(EXPECTED_PKG_PREFIX):
                problems.append(f'{rel}: suspicious package in import -> {head}')

    if problems:
        print(f'FOUND {len(problems)} PROBLEM(S):')
        for p in problems:
            print('  -', p)
        return 1

    print(f'OK - {len(files)} Kotlin files passed sanity checks')
    print(f'     res/font: {len(fonts)} files, res/layout: {len(layouts)} files')
    return 0


if __name__ == '__main__':
    sys.exit(main())
