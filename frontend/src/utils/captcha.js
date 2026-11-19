/**
 * 图形验证码（Canvas 本地生成，零后端依赖）
 * 4 位去混淆字符 + 干扰线 + 噪点 + 逐字符旋转/错位
 */
const CHARS = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'
const COLORS = ['#5b7cfa', '#8b5cf6', '#0ea5a0', '#e8890c', '#e05572', '#3d55c8']

export function drawCaptcha(canvas, dark = false) {
  const code = Array.from({ length: 4 }, () => CHARS[Math.floor(Math.random() * CHARS.length)]).join('')
  const dpr = window.devicePixelRatio || 1
  const W = 116, H = 40
  canvas.width = W * dpr
  canvas.height = H * dpr
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)

  // 底色：亮色纸感 / 暗色深底
  ctx.fillStyle = dark ? '#1a1a24' : '#f1f4ff'
  ctx.fillRect(0, 0, W, H)

  // 半调噪点
  for (let i = 0; i < 26; i++) {
    ctx.fillStyle = dark ? 'rgba(139,156,255,0.18)' : 'rgba(91,124,250,0.16)'
    ctx.beginPath()
    ctx.arc(Math.random() * W, Math.random() * H, Math.random() * 1.4 + 0.4, 0, Math.PI * 2)
    ctx.fill()
  }
  // 干扰贝塞尔线
  for (let i = 0; i < 3; i++) {
    ctx.strokeStyle = COLORS[Math.floor(Math.random() * COLORS.length)] + (dark ? '55' : '44')
    ctx.lineWidth = 1.1
    ctx.beginPath()
    ctx.moveTo(Math.random() * 20, Math.random() * H)
    ctx.bezierCurveTo(W * 0.3, Math.random() * H, W * 0.6, Math.random() * H, W - Math.random() * 20, Math.random() * H)
    ctx.stroke()
  }
  // 逐字符：随机旋转 ±22°、字号抖动、基线错位
  for (let i = 0; i < 4; i++) {
    const size = 21 + Math.random() * 5
    ctx.save()
    ctx.translate(16 + i * 26, H / 2 + (Math.random() * 8 - 4))
    ctx.rotate((Math.random() * 44 - 22) * Math.PI / 180)
    ctx.font = `700 ${size}px 'JetBrains Mono', Consolas, monospace`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillStyle = dark ? lighten(COLORS[i % COLORS.length]) : COLORS[Math.floor(Math.random() * COLORS.length)]
    ctx.fillText(code[i], 0, 0)
    ctx.restore()
  }
  return code
}

function lighten(hex) {
  const n = parseInt(hex.slice(1), 16)
  const r = Math.min(255, (n >> 16) + 70), g = Math.min(255, ((n >> 8) & 255) + 70), b = Math.min(255, (n & 255) + 70)
  return `rgb(${r},${g},${b})`
}