module.exports = {
  presets: [
    [
      '@babel/preset-env',
      {
        useBuiltIns: 'usage', // 自动检测代码中使用的 ES6+ 特性并引入对应 polyfill
        corejs: 3, // 明确使用 core-js@3（需与 package.json 中 core-js 版本匹配）
        targets: {
          browsers: ['last 2 versions', 'ie >= 11'] // 根据项目需求调整目标浏览器
        }
      }
    ],
    '@vue/app' // Vue CLI 默认 preset，确保保留
  ]
}
