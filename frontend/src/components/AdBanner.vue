<template>
  <div v-if="list.length" class="ad-strip" :class="{ foot }">
    <a v-for="(ad, idx) in list" :key="ad.id" class="ad-item flair-in"
       :class="[ad.theme || 'neutral', { hero }]"
       :href="ad.linkUrl || undefined" :target="ad.linkUrl && !isInner(ad.linkUrl) ? '_blank' : undefined"
       rel="noopener" @click="go(ad, $event)">
      <!-- hero 大横幅：流星层 + 半调装饰 + 图形 -->
      <template v-if="hero">
        <span class="ad-meteors" aria-hidden="true">
          <i v-for="n in 12" :key="n" :style="meteorStyle(n, idx)" />
        </span>
        <span class="ad-halftone" aria-hidden="true" />
        <span class="ad-deco" aria-hidden="true">
          <el-icon :size="86"><component :is="decoIcon(idx)" /></el-icon>
        </span>
        <span class="ad-hero-body">
          <span class="ad-hero-kicker">驿站精选 · SPONSORED</span>
          <b class="ad-hero-title">{{ ad.title }}</b>
          <p v-if="ad.content" class="ad-hero-sub">{{ ad.content }}</p>
          <span v-if="ad.linkUrl" class="ad-cta">
            {{ ctaText(ad) }}
            <el-icon :size="13"><ArrowRight /></el-icon>
          </span>
        </span>
        <span class="ad-tag hero-tag">推广</span>
      </template>
      <!-- 紧凑条状（侧栏 / 页脚） -->
      <template v-else>
        <div class="ad-glyph"><el-icon :size="18"><Promotion /></el-icon></div>
        <div class="ad-body">
          <b>{{ ad.title }}</b>
          <p v-if="ad.content">{{ ad.content }}</p>
        </div>
        <span class="ad-tag">推广</span>
        <el-icon v-if="isInner(ad.linkUrl)" class="ad-arrow" :size="14"><ArrowRight /></el-icon>
      </template>
    </a>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Promotion, ArrowRight, Van, Umbrella, Present, Box } from '@element-plus/icons-vue'
import { activeAds } from '../api'

const props = defineProps({
  slot: { type: String, required: true },
  foot: { type: Boolean, default: false },
  hero: { type: Boolean, default: false }
})

const router = useRouter()
const ads = ref([])
const list = computed(() => ads.value.filter(a => a.slotKey === props.slot))

/* 站内路由（如 /student/invite）走 SPA 跳转，外链才开新标签 */
const isInner = url => typeof url === 'string' && url.startsWith('/') && !url.startsWith('//')
function go(ad, e) {
  if (isInner(ad.linkUrl)) { e.preventDefault(); router.push(ad.linkUrl) }
}

/* CTA 文案按落地路由语义化 */
const CTA_MAP = [
  [/insurance/, '了解保障'],
  [/ship/, '去寄件'],
  [/invite/, '立即邀请'],
  [/wallet/, '去充值'],
  [/coupons/, '领卡券'],
  [/proxy/, '找代取']
]
function ctaText(ad) {
  const hit = CTA_MAP.find(([re]) => re.test(ad.linkUrl || ''))
  return hit ? hit[1] : '查看详情'
}

const DECOS = [Umbrella, Van, Present, Box]
const decoIcon = idx => DECOS[idx % DECOS.length]

/* 流星：确定性伪随机（避免每次渲染跳变），从右上向左下划过 */
function meteorStyle(n, idx) {
  const seed = n * 37 + idx * 11
  const left = 30 + (seed * 53) % 75          // 30% - 105%
  const delay = ((seed * 29) % 60) / 10       // 0 - 6s
  const dur = 2.6 + ((seed * 17) % 24) / 10   // 2.6 - 5s
  return { left: left + '%', animationDelay: delay + 's', animationDuration: dur + 's' }
}

onMounted(async () => {
  try { ads.value = await activeAds() } catch { ads.value = [] }
})
</script><style scoped>
.ad-strip { display: flex; flex-direction: column; gap: 8px; }
.ad-strip.foot { margin-top: 14px; }

/* ---------- 基础条状 ---------- */
.ad-item {
  position: relative; display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 12px; overflow: hidden;
  border: 1px solid var(--default-border);
  background: var(--default-box-color);
  text-decoration: none; color: var(--el-text-color-primary);
  transition: transform .25s var(--flair-ease), box-shadow .25s var(--flair-ease), border-color .25s;
}
.ad-item:hover { transform: translateY(-2px); box-shadow: var(--art-card-shadow); }
.ad-item.cool { background: linear-gradient(120deg, rgba(59,130,246,.13), rgba(59,130,246,.04)); border-color: rgba(59,130,246,.28); --ad-accent: #2563eb; --ad-meteor: rgba(37,99,235,.55); }
.ad-item.warm { background: linear-gradient(120deg, rgba(245,158,11,.14), rgba(245,158,11,.04)); border-color: rgba(245,158,11,.30); --ad-accent: #d97706; --ad-meteor: rgba(217,119,6,.5); }
.ad-item.neutral { background: linear-gradient(120deg, rgba(100,116,139,.12), rgba(100,116,139,.04)); border-color: rgba(100,116,139,.26); --ad-accent: #475569; --ad-meteor: rgba(71,85,105,.45); }
html.dark .ad-item.cool { background: linear-gradient(120deg, rgba(59,130,246,.18), rgba(30,64,175,.08)); border-color: rgba(96,165,250,.30); --ad-accent: #60a5fa; --ad-meteor: rgba(191,219,254,.7); }
html.dark .ad-item.warm { background: linear-gradient(120deg, rgba(245,158,11,.16), rgba(146,64,14,.08)); border-color: rgba(251,191,36,.28); --ad-accent: #fbbf24; --ad-meteor: rgba(253,230,138,.65); }
html.dark .ad-item.neutral { background: linear-gradient(120deg, rgba(148,163,184,.14), rgba(51,65,85,.10)); border-color: rgba(148,163,184,.26); --ad-accent: #94a3b8; --ad-meteor: rgba(226,232,240,.55); }

.ad-glyph {
  flex: none; width: 36px; height: 36px; border-radius: 10px;
  display: grid; place-items: center; color: #fff;
  background: linear-gradient(135deg, var(--ad-accent, #475569), color-mix(in srgb, var(--ad-accent, #475569) 65%, #000));
}
.ad-body { flex: 1; min-width: 0; }
.ad-body b { display: block; font-size: 13.5px; font-weight: 700; letter-spacing: .01em; }
.ad-body p { margin: 2px 0 0; font-size: 12px; color: var(--el-text-color-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ad-tag {
  flex: none; font-size: 10.5px; letter-spacing: .08em; padding: 2px 7px; border-radius: 999px;
  color: var(--el-text-color-secondary); border: 1px solid var(--default-border);
  background: color-mix(in srgb, var(--default-box-color) 65%, transparent);
}
.ad-arrow { flex: none; color: var(--ad-accent); transition: transform .25s var(--flair-ease); }
.ad-item:hover .ad-arrow { transform: translateX(3px); }

/* ---------- hero 大横幅 ---------- */
.ad-item.hero {
  min-height: 136px; align-items: stretch; padding: 20px 24px;
  border-radius: 16px; cursor: pointer;
}
.ad-hero-body { position: relative; z-index: 2; display: flex; flex-direction: column; align-items: flex-start; gap: 6px; max-width: 72%; }
.ad-hero-kicker { font-size: 10.5px; font-weight: 600; letter-spacing: .18em; color: var(--ad-accent); opacity: .9; }
.ad-hero-title { font-size: 18px; font-weight: 800; letter-spacing: .01em; line-height: 1.3; }
.ad-hero-sub { margin: 0; font-size: 12.5px; line-height: 1.6; color: var(--el-text-color-regular); }
.ad-cta {
  margin-top: 6px; display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 15px; border-radius: 999px; font-size: 12.5px; font-weight: 600;
  color: #fff; background: var(--ad-accent);
  box-shadow: 0 6px 16px -6px color-mix(in srgb, var(--ad-accent) 65%, transparent);
  transition: transform .25s var(--flair-ease), box-shadow .25s var(--flair-ease);
}
html.dark .ad-cta { color: #0b1220; }
.ad-item.hero:hover .ad-cta { transform: translateX(3px); box-shadow: 0 10px 22px -8px color-mix(in srgb, var(--ad-accent) 75%, transparent); }
.hero-tag { position: absolute; top: 12px; right: 12px; z-index: 2; }

/* 半调点阵装饰（右侧渐隐） */
.ad-halftone {
  position: absolute; inset: 0 0 0 auto; width: 46%; z-index: 0; pointer-events: none;
  background-image: radial-gradient(circle, currentColor 1px, transparent 1.4px);
  background-size: 12px 12px; opacity: .07;
  -webkit-mask-image: linear-gradient(to left, #000 30%, transparent);
  mask-image: linear-gradient(to left, #000 30%, transparent);
}
/* 右下角大图标剪影 */
.ad-deco {
  position: absolute; right: 22px; bottom: -16px; z-index: 1; pointer-events: none;
  color: var(--ad-accent); opacity: .16; transform: rotate(-8deg);
  transition: transform .4s var(--flair-ease), opacity .4s;
}
.ad-item.hero:hover .ad-deco { transform: rotate(-4deg) scale(1.06); opacity: .22; }

/* 流星层：右上 → 左下 */
.ad-meteors { position: absolute; inset: 0; z-index: 1; overflow: hidden; pointer-events: none; }
.ad-meteors i {
  position: absolute; top: -12px; width: 88px; height: 1.5px; border-radius: 2px;
  background: linear-gradient(to right, transparent, var(--ad-meteor, rgba(255,255,255,.7)));
  opacity: 0; transform: rotate(145deg) translateX(0);
  animation: ad-meteor linear infinite;
}
.ad-meteors i::after {
  content: ''; position: absolute; right: -1px; top: 50%; width: 3px; height: 3px;
  border-radius: 50%; transform: translateY(-50%);
  background: var(--ad-meteor, rgba(255,255,255,.8));
}
@keyframes ad-meteor {
  0% { transform: rotate(145deg) translateX(0); opacity: 0; }
  8% { opacity: .9; }
  62% { opacity: .9; }
  100% { transform: rotate(145deg) translateX(340px); opacity: 0; }
}

@media (max-width: 640px) {
  .ad-item.hero { min-height: 116px; padding: 16px 18px; }
  .ad-hero-body { max-width: 88%; }
  .ad-hero-title { font-size: 16px; }
  .ad-deco { right: 8px; bottom: -22px; opacity: .1; }
  .ad-halftone { width: 34%; }
}
@media (prefers-reduced-motion: reduce) {
  .ad-meteors i { animation: none; opacity: 0; }
  .ad-item, .ad-cta, .ad-deco, .ad-arrow { transition: none; }
}
</style>