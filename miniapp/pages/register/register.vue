<template>
  <view class="register-page">
    <view class="logo">
      <text class="title">注册账号</text>
    </view>

    <view class="form card">
      <view class="form-item">
        <text class="label">用户名</text>
        <input class="input" placeholder="请输入用户名" v-model="form.username" />
      </view>

      <view class="form-item">
        <text class="label">密码</text>
        <input class="input" placeholder="请输入密码" v-model="form.password" password />
      </view>

      <view class="form-item">
        <text class="label">确认密码</text>
        <input class="input" placeholder="请再次输入密码" v-model="form.confirmPassword" password />
      </view>

      <view class="form-item">
        <text class="label">邮箱</text>
        <input class="input" placeholder="请输入邮箱" v-model="form.email" />
      </view>

      <button class="btn-primary" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <view class="login-link" @click="goLogin">
        已有账号？去登录
      </view>
    </view>
  </view>
</template>

<script>
import { register } from '../../api/user'

export default {
  data () {
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        email: ''
      },
      loading: false
    }
  },
  methods: {
    /**
     * 表单校验
     */
    validateForm () {
      if (!this.form.username) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return false
      }
      if (this.form.username.length < 3) {
        uni.showToast({ title: '用户名至少 3 个字符', icon: 'none' })
        return false
      }
      if (!this.form.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return false
      }
      if (this.form.password.length < 6) {
        uni.showToast({ title: '密码至少 6 个字符', icon: 'none' })
        return false
      }
      if (this.form.password !== this.form.confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return false
      }
      if (!this.form.email) {
        uni.showToast({ title: '请输入邮箱', icon: 'none' })
        return false
      }
      const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailReg.test(this.form.email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return false
      }
      return true
    },

    /**
     * 处理注册
     */
    async handleRegister () {
      if (!this.validateForm()) return
      this.loading = true
      try {
        await register({
          username: this.form.username,
          password: this.form.password,
          email: this.form.email
        })
        uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (e) {
        // 错误已由 request.js 统一提示
      } finally {
        this.loading = false
      }
    },

    /**
     * 返回登录页
     */
    goLogin () {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss">
.register-page {
  min-height: 100vh;
  padding: 60rpx 32rpx;
}

.logo {
  text-align: center;
  margin: 40rpx 0;

  .title {
    font-size: 48rpx;
    font-weight: bold;
    color: #1890ff;
  }
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
    margin: 20rpx 0;
  }

  .login-link {
    text-align: center;
    color: #1890ff;
    font-size: 28rpx;
    margin-top: 24rpx;
  }
}
</style>
