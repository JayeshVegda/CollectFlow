import React from 'react';
import {Composition} from 'remotion';
import {NativeAppPromo} from './video/NativeAppPromo';

export const Root: React.FC = () => (
  <Composition
    id="NativeAppPromo"
    component={NativeAppPromo}
    durationInFrames={360}
    fps={30}
    width={1920}
    height={1080}
  />
);
