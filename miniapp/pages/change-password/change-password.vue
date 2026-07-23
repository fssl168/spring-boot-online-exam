<template>
  <view class="change-password-page">
    <view class="form card">
      <view class="form-item">
        <text class="label">旧密码</text>
        <input class="input" placeholder="请输入旧密码" v-model="form.oldPassword" password />
      </view>

      <view class="form-item">
        <text class="label">新密码</text>
        <input class="input" placeholder="请输入新密码（至少 6 位）" v-model="form.newPassword" password />
      </view>

      <view class="form-item">
        <text class="label">确认新密码</text>
        <input class="input" placeholder="请再次输入新密码" v-model="form.confirmPassword" password />
      </view>

      <button class="btn-primary" :disabled="loading" @click="handleSubmit">
        {{ loading ? '提交中...' : '确认修改' }}
      </button>
    </view>
  </view>
</template>

<script>
import { changePassword } from '../../api/user'

export default {
  data () {
    return {
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      loading: false
    }
  },
  methods: {
    /**
     * 表单校验
     */
    validateForm () {
      if (!this.form.oldPassword) {
        uni.showToast({ title: '请输入旧密码', icon: 'none' })
        return false
      }
      if (!this.form.newPassword) {
        uni.showToast({ title: '请输入新密码', icon: 'none' })
        return false
      }
      if (this.form.newPassword.length < 6) {
        uni.showToast({ title: '新密码至少 6 位', icon: 'none' })
        return false
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return false
      }
      if (this.form.oldPassword === this.form.newPassword) {
        uni.showToast({ title: '新密码不能与旧密码相同', icon: 'none' })
        return false
      }
      return true
    },

    /**
     * 提交修改
     */
    async handleSubmit () {
      if (!this.validateForm()) return
      this.loading = true
      try {
        await changePassword({
          oldPassword: this.form.oldPassword,
          newPassword: this.form.newPassword
        })
        uni.showModal({
          title: '修改成功',
          content: '密码已修改，请重新登录',
          showCancel: false,
          success: () => {
            // 退出登录
            this.$store.dispatch('user/logout')
            // 跳转登录页
            uni.reLaunch({ url: '/pages/login/login' })
          }
        })
      } catch (e) {
        // 错误已统一处理
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss">
.change-password-page {
  min-height: 100vh;
  padding: 24rpx;
}

.form {
  padding: 40rpx 32rpx;

  .form-item {
    margin-bottom: 32rpx;

    .label {
      display: block;
      font-size: 28rpx;
      color: #666;
      margin-bottom: 12rpx;
    }

    .input {
      width: 100%;
      height: 88rpx;
      border: 1rpx solid #e8e8e8;
      border-radius: 8rpx;
      padding: 0 24rpx;
      font-size: 30rpx;
      box-sizing: border-box;
    }
  }

  .btn-primary {
    width: 100%;
    margin-top: 20rpx;
  }
}
</style>
