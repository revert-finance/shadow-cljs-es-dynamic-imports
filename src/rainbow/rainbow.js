/* Use with vite to generate a umd lib because async imports do not work with cljs compiler */
import './polyfills';
import '@rainbow-me/rainbowkit/styles.css';
import * as rainbowkit from '@rainbow-me/rainbowkit';

export default rainbowkit
