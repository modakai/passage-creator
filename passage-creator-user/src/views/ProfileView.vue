<template>
  <div class="space-y-8">
    <section>
      <h1 class="text-4xl font-semibold tracking-[-0.06em] text-slate-950 sm:text-5xl">个人中心</h1>
      <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-500">用 Apple 设置页式结构管理创作偏好，不进入后台配置语境。</p>
    </section>

    <section class="grid gap-6 lg:grid-cols-[320px_minmax(0,1fr)]">
      <aside class="glass-panel rounded-[2rem] p-6 text-center">
        <div class="mx-auto grid size-24 place-items-center rounded-full bg-slate-950 text-3xl font-semibold text-white">S</div>
        <h2 class="mt-5 text-2xl font-semibold tracking-[-0.04em]">Sakura</h2>
        <p class="mt-2 text-sm text-slate-500">sakura@example.com</p>
        <span class="mt-5 inline-flex rounded-full border border-emerald-100 bg-emerald-50 px-3 py-1.5 text-sm text-emerald-700">Creator Pro</span>
      </aside>

      <div class="space-y-5">
        <section v-for="group in [
          { title: '创作偏好', items: ['默认模型：GPT-4.1', '默认写作风格：专业自然', '默认文章长度：1500 - 2000 字', '默认生成配图：开启'] },
          { title: '通知设置', items: ['任务完成提醒：开启', '等待确认提醒：开启', '额度不足提醒：开启'] },
          { title: '账号安全', items: ['修改密码', '登录设备：2 台设备', '退出登录'] }
        ]" :key="group.title" class="glass-panel overflow-hidden rounded-[2rem]">
          <h3 class="border-b border-slate-100 px-6 py-4 text-sm font-semibold text-slate-500">{{ group.title }}</h3>
          <div>
            <button v-for="item in group.items" :key="item" type="button" class="flex w-full items-center justify-between border-b border-slate-100 px-6 py-4 text-left text-sm last:border-b-0 hover:bg-white/60">
              <span class="font-medium text-slate-800">{{ item }}</span>
              <span class="text-slate-300">›</span>
            </button>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>
