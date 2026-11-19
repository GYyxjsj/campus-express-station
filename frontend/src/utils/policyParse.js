/** 协议正文解析：`# 标题` 分节，`1.1 条款` 自动编号 */
export function parsePolicy(text) {
  const out = []
  let sec = null
  for (const raw of String(text || '').split('\n')) {
    const line = raw.trim()
    if (!line) continue
    if (line.startsWith('#')) {
      sec = { title: line.replace(/^#+\s*/, ''), clauses: [] }
      out.push(sec)
      continue
    }
    if (!sec) { sec = { title: '正文', clauses: [] }; out.push(sec) }
    const m = line.match(/^(\d+\.\d+)\s*(.*)$/)
    sec.clauses.push(m ? { no: m[1], text: m[2] } : { no: '', text: line })
  }
  return out
}

export const POLICY_TEMPLATE = `# 一、总则
1.1 本协议适用于校园快递驿站全体注册用户，注册即视为已阅读并同意本协议全部条款。
1.2 驿站保留在法律法规允许范围内修订本协议的权利，修订后将以站内提醒与公告弹窗通知。

# 二、服务内容
2.1 驿站提供包裹代收、上架保管、到站提醒、取件核销与代取授权等基础服务。
2.2 增值服务（付费代取、钱包充值）需另行签署对应专项规则。

# 三、用户义务
3.1 用户应保证注册信息真实有效，手机号须与包裹收件手机号一致，否则无法自动关联包裹。
3.2 用户不得利用代取服务从事违禁品寄递、刷单套现等违规行为。

# 四、责任与免责
4.1 包裹到站后免费保管 3 天，超期将标记滞留并按策略催领，长期无人领取可退回承运商。
4.2 因用户自身泄露取件码、身份条码或授权码导致的损失，由用户自行承担。

# 五、协议生效
5.1 本协议自发布之日起生效，必签协议未签署将限制相关服务的使用。`