// vite.config.js
import { defineConfig } from 'vite'
import babel from 'vite-plugin-babel'

export default defineConfig({
  plugins: [babel({ babelConfig: {
babelrc: false,
        configFile: false,
    plugins: ["babel-plugin-transform-dynamic-imports-to-static-imports"] } })],
  publicDir: false,
  build: {
    minify: false,
    assetsDir: "rainbow",
    outDir: "src/gen",
    lib: {
      entry: "src/rainbow/rainbow",
      name: 'rainbowkit',
      formats: ['cjs'],
      // the proper extensions will be added
      fileName: 'rainbowkit'
    },
    rollupOptions: {
      // make sure to externalize deps that shouldn't be bundled
      // into your library
      external: ['react', 'react-dom', 'wagmi', 'ethers']
    }
  }
})
