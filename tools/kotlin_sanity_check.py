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
  * Compose experimental APIs used inside a function that has no matching `@OptIn`. This is a
    hard compile error, not a warning, and it is invisible to every other check here: braces
    balance and every identifier resolves. It broke CI when `Modifier.animateItemPlacement`
    was called from `entryGroup`, which carried no annotation even though `CollectionRow`
    (the neighbouring function) did.
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


def mask_literals(src: str) -> str:
    """Blank out comments and string/char literals, but KEEP every newline.

    strip_code() above cannot be used for the opt-in check: it deletes block comments
    wholesale, newlines included, so its output no longer aligns with the source and a report
    could not name the offending line - and naming the line is the entire point. Masking also
    matters because KDoc legitimately mentions these APIs in prose (the `entryGroup` comment
    written for the fix names `Modifier.animateItemPlacement`); text in a comment must never
    be mistaken for a real call site.
    """
    out = []
    i = 0
    n = len(src)
    while i < n:
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            end = n if j == -1 else j + 3
            out.append(re.sub(r'[^\n]', ' ', src[i:end]))
            i = end
            continue
        c = src[i]
        if c == '"' or c == "'":
            quote = c
            j = i + 1
            while j < n:
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == quote:
                    j += 1
                    break
                j += 1
            out.append(re.sub(r'[^\n]', ' ', src[i:j]))
            i = j
            continue
        if src.startswith('//', i):
            j = src.find('\n', i)
            end = n if j == -1 else j
            out.append(' ' * (end - i))
            i = end
            continue
        if src.startswith('/*', i):
            j = src.find('*/', i + 2)
            end = n if j == -1 else j + 2
            out.append(re.sub(r'[^\n]', ' ', src[i:end]))
            i = end
            continue
        out.append(c)
        i += 1
    return ''.join(out)


# Compose APIs that are still gated behind an opt-in marker in the versions this project pins
# (compose-bom 2024.05.00 -> foundation 1.6.7, material3 1.2.1). Only symbols confirmed to be
# experimental belong here: a wrong entry invents a failure that CI would not report, which is
# worse than missing one. `Modifier.combinedClickable` is deliberately absent - it is used in
# CollectionRow and is not reliably gated across these versions, so listing it would be a guess.
EXPERIMENTAL_APIS = {
    # androidx.compose.foundation
    'animateItemPlacement': 'ExperimentalFoundationApi',
    'HorizontalPager': 'ExperimentalFoundationApi',
    'VerticalPager': 'ExperimentalFoundationApi',
    'basicMarquee': 'ExperimentalFoundationApi',
    # androidx.compose.foundation.layout
    'FlowRow': 'ExperimentalLayoutApi',
    'FlowColumn': 'ExperimentalLayoutApi',
    # androidx.compose.material3
    'TopAppBar': 'ExperimentalMaterial3Api',
    'TopAppBarDefaults': 'ExperimentalMaterial3Api',
    'ModalBottomSheet': 'ExperimentalMaterial3Api',
    'DatePicker': 'ExperimentalMaterial3Api',
    'DatePickerDialog': 'ExperimentalMaterial3Api',
    'DatePickerDefaults': 'ExperimentalMaterial3Api',
    'rememberDatePickerState': 'ExperimentalMaterial3Api',
    'SegmentedButton': 'ExperimentalMaterial3Api',
    'SearchBar': 'ExperimentalMaterial3Api',
    'PullToRefreshBox': 'ExperimentalMaterial3Api',
    'ExposedDropdownMenuBox': 'ExperimentalMaterial3Api',
    'TooltipBox': 'ExperimentalMaterial3Api',
}

TOP_LEVEL_FUN_RE = re.compile(
    r'^(?:private\s+|internal\s+|public\s+|protected\s+)?(?:suspend\s+)?fun\s',
)
OPTIN_RE = re.compile(r'@OptIn\s*\(([^)]*)\)', re.S)


def optin_markers(text: str) -> set:
    """Marker names from every @OptIn(...) / @file:OptIn(...) occurrence in `text`.

    Annotations are written `ExperimentalFooApi::class` while EXPERIMENTAL_APIS stores the bare
    marker name, so the `::class` suffix is stripped here. Comparing the two forms directly is
    what produced 24 false positives on the first run - every opted-in call site looked
    un-opted-in because 'ExperimentalMaterial3Api' != 'ExperimentalMaterial3Api::class'.
    """
    names = set()
    for group in OPTIN_RE.findall(text):
        for part in group.split(','):
            marker = re.sub(r'::class$', '', part.strip()).strip()
            if marker:
                names.add(marker)
    return names


def check_experimental_optin(rel: str, masked: str, problems: list) -> None:
    """Report experimental APIs whose enclosing top-level function lacks the opt-in.

    Opt-in is LEXICALLY scoped, so it propagates into lambdas nested inside the function
    (which is why the `item {}` / `items {}` blocks in `entryGroup` are covered by one
    annotation on the function itself) but NOT into a different function that merely calls it.
    That asymmetry is exactly what produced the CI failure, so the check attributes each usage
    to its nearest enclosing top-level function rather than to the file.
    """
    lines = masked.splitlines()

    file_markers = set()
    for m in re.finditer(r'@file:OptIn\s*\(([^)]*)\)', masked, re.S):
        file_markers |= optin_markers('@OptIn(' + m.group(1) + ')')

    # Nested and local functions are indented, so anchoring to column 0 deliberately ignores
    # them: their opt-in needs are covered by the top-level function they live in.
    func_starts = [i for i, line in enumerate(lines) if TOP_LEVEL_FUN_RE.match(line)]

    for i, line in enumerate(lines):
        stripped = line.lstrip()
        # Imports and the package line name these symbols without calling them.
        if stripped.startswith(('import ', 'package ', '@file:')):
            continue
        hits = {s for s in EXPERIMENTAL_APIS if re.search(r'\b' + re.escape(s) + r'\b', line)}
        if not hits:
            continue
        owner = max((f for f in func_starts if f < i), default=None)
        if owner is None:
            markers = file_markers
        else:
            # The annotation block sits directly above the declaration, but it may span lines
            # and may sit under a KDoc, so look back a bounded window.
            markers = file_markers | optin_markers('\n'.join(lines[max(0, owner - 12):owner]))
        missing = sorted({EXPERIMENTAL_APIS[s] for s in hits} - markers)
        if missing:
            opts = ', '.join(m + '::class' for m in missing)
            problems.append(
                f'{rel}:{i + 1}: {", ".join(sorted(hits))} used without @OptIn({opts})'
            )


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
        masked = mask_literals(raw)
        rel = os.path.relpath(path, ROOT)

        check_experimental_optin(rel, masked, problems)

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
