// Тема выставляется до первой отрисовки, чтобы тёмная тема не мигала светлой.
// Ключ и значения совпадают с ThemeProvider (src/theme/themeContext.ts, ключ «ct.theme»).
// Лежит отдельным файлом, а не inline-скриптом в index.html, чтобы работала строгая CSP (script-src 'self').
;(function () {
  try {
    var saved = localStorage.getItem('ct.theme') || 'system'
    var dark =
      saved === 'dark' ||
      (saved === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    var root = document.documentElement
    root.classList.add(dark ? 'dark' : 'light')
    root.style.colorScheme = dark ? 'dark' : 'light'
  } catch (e) {
    // хранилище недоступно — тему выставит ThemeProvider
  }
})()
