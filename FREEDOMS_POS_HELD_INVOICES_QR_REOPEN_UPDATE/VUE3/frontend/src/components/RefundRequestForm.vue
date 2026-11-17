<script setup>
import { computed, ref, watch } from 'vue'
const props = defineProps({ order: { type: Object, required: true } })
const emit = defineEmits(['submit', 'cancel'])
const fullRefund = ref(true)
const amount = ref(Number(props.order.refundEligibility?.remainingAmount || 0))
const reason = ref('')
const remaining = computed(() => Number(props.order.refundEligibility?.remainingAmount || 0))
const requestsRemaining = computed(() => Number(props.order.refundEligibility?.requestsRemaining ?? 0))
const deadlineText = computed(() => props.order.refundEligibility?.deadline ? new Date(props.order.refundEligibility.deadline).toLocaleString('vi-VN') : '—')
watch(() => props.order.refundEligibility?.remainingAmount, value => { amount.value = Number(value || 0) })
function submit() {
  if (!reason.value.trim()) return
  const value = fullRefund.value ? null : Number(amount.value)
  if (!fullRefund.value && (!Number.isFinite(value) || value <= 0 || value > remaining.value)) return
  emit('submit', { fullRefund: fullRefund.value, amount: value, reason: reason.value.trim() })
}
</script>

<template>
  <section class="refund-request-form">
    <div class="refund-form-heading"><strong>Gửi yêu cầu hoàn tiền · {{ order.code }}</strong><button type="button" @click="emit('cancel')" aria-label="Đóng">×</button></div>
    <div class="refund-form-facts"><span>Giới hạn hoàn còn lại <b>{{ remaining.toLocaleString('vi-VN') }}₫</b></span><span>Lượt yêu cầu còn lại <b>{{ requestsRemaining }}/2</b></span><span>Hạn gửi <b>{{ deadlineText }}</b></span></div>
    <div class="refund-form-options">
      <label><input v-model="fullRefund" type="radio" :value="true" /> Hoàn toàn phần <small>{{ remaining.toLocaleString('vi-VN') }}₫</small></label>
      <label><input v-model="fullRefund" type="radio" :value="false" /> Hoàn một phần</label>
    </div>
    <label v-if="!fullRefund" class="refund-form-field">Số tiền muốn hoàn (VNĐ)
      <input v-model.number="amount" type="number" min="1" :max="remaining" step="1" required />
      <small>Không vượt quá {{ remaining.toLocaleString('vi-VN') }}₫ còn có thể hoàn.</small>
    </label>
    <label class="refund-form-field">Lý do yêu cầu
      <textarea v-model.trim="reason" rows="2" maxlength="500" placeholder="Mô tả lý do yêu cầu hoàn tiền…" required></textarea>
    </label>
    <div class="refund-form-actions"><button type="button" class="refund-form-cancel" @click="emit('cancel')">Để sau</button><button type="button" class="refund-form-submit" :disabled="!reason.trim() || (!fullRefund && (!amount || amount <= 0 || amount > remaining))" @click="submit">Gửi yêu cầu hoàn tiền</button></div>
  </section>
</template>

<style scoped>
.refund-request-form{margin-top:14px;padding:16px;border:1px solid #e5e5e5;border-radius:8px;background:#fafafa;color:#111}.refund-form-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.refund-form-heading strong{font-size:12px}.refund-form-heading button{width:25px;height:25px;border:0;border-radius:50%;background:#eee;color:#222;font-size:18px;cursor:pointer}.refund-form-facts{display:flex;flex-wrap:wrap;gap:8px 18px;margin:12px 0;font-size:10px;color:#666}.refund-form-facts b{display:block;color:#111;font-size:11px;margin-top:3px}.refund-form-options{display:flex;flex-wrap:wrap;gap:10px;margin:12px 0}.refund-form-options label{display:flex;align-items:center;gap:7px;padding:9px 11px;background:#fff;border:1px solid #ddd;border-radius:5px;font-size:11px}.refund-form-options input{accent-color:#111}.refund-form-options small{color:#666}.refund-form-field{display:flex;flex-direction:column;gap:6px;margin:10px 0;font-size:10px;font-weight:800}.refund-form-field input,.refund-form-field textarea{width:100%;box-sizing:border-box;padding:9px 10px;border:1px solid #ddd;border-radius:5px;background:#fff;color:#111;font:inherit;font-size:11px}.refund-form-field small{font-size:9px;color:#777;font-weight:400}.refund-form-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:12px}.refund-form-cancel,.refund-form-submit{padding:9px 13px;border-radius:4px;border:1px solid #111;font-size:10px;font-weight:800;cursor:pointer}.refund-form-cancel{background:#fff;color:#111}.refund-form-submit{background:#111;color:#fff}.refund-form-submit:disabled{opacity:.45;cursor:not-allowed}
</style>
