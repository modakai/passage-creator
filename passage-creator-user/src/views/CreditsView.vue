<template>
  <div class="space-y-8">
    <section>
      <h1 class="text-4xl font-semibold tracking-[-0.06em] text-slate-950 sm:text-5xl">额度与会员</h1>
      <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">当前后端已有积分计费能力，独立用户端先以清爽信息面板承载，支付升级放入后续任务。</p>
    </section>

    <section class="grid gap-6 lg:grid-cols-[1.2fr_.8fr]">
      <div class="glass-panel rounded-[2rem] p-6">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-2xl font-semibold tracking-[-0.04em]">当前额度</h2>
            <p class="mt-2 text-sm text-slate-500">Creator Pro · 有效期至 2026-06-29</p>
          </div>
          <span class="rounded-full border border-emerald-100 bg-emerald-50 px-3 py-1.5 text-sm text-emerald-700">正常</span>
        </div>
        <div class="mt-6 grid grid-cols-2 gap-3">
          <div v-for="item in [{ n: '8,420', t: '剩余额度' }, { n: '1,580', t: '本月已用' }, { n: '86', t: '生成图片' }, { n: '42h', t: '节省时间' }]" :key="item.t" class="rounded-3xl bg-white/75 p-5">
            <b class="text-3xl tracking-[-0.06em]">{{ item.n }}</b>
            <span class="mt-2 block text-sm text-slate-500">{{ item.t }}</span>
          </div>
        </div>
      </div>

      <div class="glass-panel rounded-[2rem] p-6">
        <h2 class="text-2xl font-semibold tracking-[-0.04em]">最近消耗</h2>
        <div class="mt-5 space-y-3">
          <div v-for="item in ['AI 文章生成 · 24 credits', '小红书笔记 · 8 credits', '图片生成 · 18 credits']" :key="item" class="rounded-3xl border border-slate-200 bg-white/70 p-4 text-sm text-slate-600">{{ item }}</div>
        </div>
      </div>
    </section>

    <section class="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
      <article v-for="plan in ['Free', 'Creator', 'Pro', 'Team']" :key="plan" class="glass-panel rounded-[2rem] p-6">
        <h3 class="text-xl font-semibold">{{ plan }}</h3>
        <p class="mt-2 text-sm leading-6 text-slate-500">适合不同频率的 AI 创作者。</p>
        <div class="my-6 text-3xl font-semibold tracking-[-0.06em]">{{ plan === 'Free' ? '¥0' : plan === 'Team' ? '定制' : plan === 'Creator' ? '¥39/月' : '¥99/月' }}</div>
        <button type="button" class="w-full rounded-2xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white">{{ plan === 'Free' ? '当前方案' : '升级方案' }}</button>
      </article>
    </section>
  </div>
</template>
