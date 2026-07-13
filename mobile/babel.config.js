module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      ['babel-preset-expo', { jsxImportSource: 'nativewind' }],
      'nativewind/babel',
    ],
    plugins: [
      // Reanimated 4 delegates its worklet transform to react-native-worklets.
      // This plugin must be listed LAST.
      'react-native-worklets/plugin',
    ],
  };
};
