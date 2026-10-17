import React from 'react';
import {AbsoluteFill, Sequence, useCurrentFrame} from 'remotion';
import {lerp, progress} from './motion';

const C = {
  paper: '#F1EDE3',
  ink: '#171716',
  muted: '#77736B',
  line: 'rgba(23,23,22,0.13)',
  accent: '#6A5B8D',
  warm: '#B95F4A',
};

const Paper: React.FC<React.PropsWithChildren> = ({children}) => {
  const frame = useCurrentFrame();
  const glowX = 50 + Math.sin(frame / 80) * 3;
  return (
    <AbsoluteFill
      style={{
        backgroundColor: C.paper,
        backgroundImage: `radial-gradient(circle at ${glowX}% 28%, rgba(255,255,255,.84), transparent 42%), linear-gradient(90deg, rgba(0,0,0,.018) 1px, transparent 1px)`,
        backgroundSize: 'auto, 9px 9px',
        color: C.ink,
        fontFamily: 'Inter, ui-sans-serif, system-ui, sans-serif',
        overflow: 'hidden',
      }}
    >
      {children}
    </AbsoluteFill>
  );
};

const Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = progress(frame, 6, 38);
  const underline = progress(frame, 42, 84);
  return (
    <AbsoluteFill style={{padding: '112px 128px', justifyContent: 'center'}}>
      <div style={{fontSize: 25, letterSpacing: 5, color: C.accent, opacity: enter}}>NATIVE APP / PRODUCT FILM</div>
      <div
        style={{
          fontFamily: 'Georgia, serif',
          fontSize: 112,
          letterSpacing: -5,
          lineHeight: 1.02,
          maxWidth: 1380,
          marginTop: 26,
          opacity: enter,
          transform: `translate3d(0, ${lerp(44, 0, enter)}px, 0)`,
        }}
      >
        Let the product<br />direct the film.
      </div>
      <div style={{marginTop: 54, width: `${lerp(0, 720, underline)}px`, height: 4, background: C.warm}} />
    </AbsoluteFill>
  );
};

const Proof: React.FC = () => {
  const frame = useCurrentFrame();
  const phone = progress(frame, 4, 40);
  const evidence = progress(frame, 44, 92);
  const settleFrame = Math.min(frame, 112);
  const camera = progress(settleFrame, 20, 112);
  return (
    <AbsoluteFill style={{padding: '92px 120px', flexDirection: 'row', alignItems: 'center', gap: 110}}>
      <div
        style={{
          width: 475,
          height: 890,
          borderRadius: 76,
          background: C.ink,
          padding: 14,
          boxShadow: '0 42px 100px rgba(25,23,19,.22)',
          opacity: phone,
          transform: `translate3d(0, ${lerp(40, -8, camera)}px, 0) rotate(${lerp(2, -1, camera)}deg)`,
        }}
      >
        <div style={{height: '100%', borderRadius: 63, background: '#FAF8F2', padding: '95px 38px 36px'}}>
          <div style={{fontSize: 21, color: C.muted}}>REAL NATIVE STATE</div>
          <div style={{fontFamily: 'Georgia, serif', fontSize: 54, lineHeight: 1.08, marginTop: 24}}>Place a sanitized app capture here.</div>
          <div style={{marginTop: 44, border: `1px solid ${C.line}`, borderRadius: 30, padding: 28}}>
            <div style={{fontSize: 24, color: C.muted}}>Evidence ID</div>
            <div style={{fontSize: 38, marginTop: 14}}>FEATURE-01</div>
          </div>
        </div>
      </div>
      <div style={{flex: 1}}>
        <div style={{fontSize: 27, letterSpacing: 4, color: C.warm}}>PROVE, THEN EXPLAIN</div>
        <div style={{fontFamily: 'Georgia, serif', fontSize: 82, lineHeight: 1.06, marginTop: 22}}>Native truth becomes cinematic motion.</div>
        <div style={{display: 'grid', gap: 18, marginTop: 54}}>
          {['Source or runtime evidence', 'Frame-driven HTML explanation', 'Readable settle and semantic handoff'].map((label, index) => {
            const item = progress(frame, 48 + index * 12, 76 + index * 12);
            return (
              <div
                key={label}
                style={{
                  borderTop: `1px solid ${C.line}`,
                  paddingTop: 18,
                  fontSize: 31,
                  opacity: item * evidence,
                  transform: `translate3d(${lerp(34, 0, item)}px, 0, 0)`,
                }}
              >
                <span style={{color: C.accent, marginRight: 24}}>0{index + 1}</span>{label}
              </div>
            );
          })}
        </div>
      </div>
    </AbsoluteFill>
  );
};

const Close: React.FC = () => {
  const frame = useCurrentFrame();
  const enter = progress(frame, 2, 30);
  const rule = progress(frame, 22, 64);
  return (
    <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', textAlign: 'center'}}>
      <div style={{fontSize: 24, letterSpacing: 5, color: C.accent, opacity: enter}}>EVIDENCE / MOTION / SOUND / QA</div>
      <div style={{fontFamily: 'Georgia, serif', fontSize: 100, marginTop: 26, opacity: enter, transform: `translate3d(0, ${lerp(28, 0, enter)}px, 0)`}}>
        Build the story the app deserves.
      </div>
      <div style={{height: 3, width: `${lerp(0, 560, rule)}px`, background: C.warm, marginTop: 48}} />
    </AbsoluteFill>
  );
};

export const NativeAppPromo: React.FC = () => (
  <Paper>
    <Sequence from={0} durationInFrames={120}><Intro /></Sequence>
    <Sequence from={120} durationInFrames={150}><Proof /></Sequence>
    <Sequence from={270} durationInFrames={90}><Close /></Sequence>
  </Paper>
);
