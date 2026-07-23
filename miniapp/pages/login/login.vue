<template>
  <view class="login-page">
    <view class="logo">
      <text class="title">在线考试系统</text>
      <text class="subtitle">学生端登录</text>
    </view>

    <view class="form card">
      <!-- 登录方式切换 -->
      <view class="login-type">
        <text
          :class="['tab', loginType === 1 ? 'active' : '']"
          @click="switchLoginType(1)"
        >用户名登录</text>
        <text
          :class="['tab', loginType === 2 ? 'active' : '']"
          @click="switchLoginType(2)"
        >邮箱登录</text>
      </view>

      <!-- 用户名/邮箱输入 -->
      <view class="form-item">
        <input
          class="input"
          :placeholder="loginType === 1 ? '请输入用户名' : '请输入邮箱'"
          v-model="form.userInfo"
          :type="loginType === 2 ? 'text' : 'text'"
        />
      </view>

      <!-- 密码输入 -->
      <view class="form-item">
        <input
          class="input"
          placeholder="请输入密码"
          v-model="form.password"
          password
        />
      </view>

      <!-- 登录按钮 -->
      <button class="btn-primary" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <!-- 注册链接 -->
      <view class="register-link" @click="goRegister">
        还没有账号？去注册
      </view>
    </view>
  </view>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  data () {
    return {
      loginType: 1, // 1=用户名，2=邮箱
      form: {
        userInfo: '',
        password: ''
      },
      loading: false
    }
  },
  methods: {
    ...mapActions('user', ['login']),

    /**
     * 切换登录方式
     */
    switchLoginType (type) {
      this.loginType = type
      this.form.userInfo = ''
    },

    /**
     * 表单校验
     */
    validateForm () {
      if (!this.form.userInfo) {
        uni.showToast({ title: this.loginType === 1 ? '请输入用户名' : '请输入邮箱', icon: 'none' })
        return false
      }
      if (this.loginType === 2) {
        const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailReg.test(this.form.userInfo)) {
          uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
          return false
        }
      }
      if (!this.form.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return false
      }
      return true
    },

    /**
     * 处理登录
     */
    async handleLogin () {
      if (!this.validateForm()) return
      this.loading = true
      try {
        await this.login({
          loginType: this.loginType,
          userInfo: this.form.userInfo,
          password: this.form.password
        })
        uni.showToast({ title: '登录成功', icon: 'success' })
        // 登录成功后刷新用户信息
        await this.$store.dispatch('user/refreshUserInfo')
        // 跳转首页（tabBar 页用 switchTab）
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      } catch (e) {
        // 错误已由 request.js 统一提示
      } finally {
        this.loading = false
      }
    },

    /**
     * 跳转注册页
     */
    goRegister () {
      uni.navigateTo({ url: '/pages/register/register' })
    }
  }
}
</script>

<style lang="scss">
.login-page {
  min-height: 100vh;
  padding: 60rpx 32rpx;
}

.logo {
  text-align: center;
  margin: 60rpx 0;

  .title {
    display: block;
    font-size: 48rpx;
    font-weight: bold;
    color: #1890ff;
    margin-bottom: 16rpx;
  }

  .subtitle {
    display: block;
    font-size: 28rpx;
    color: #999;
  }
}

.form {
  padding: 40rpx 32rpx;

  .login-type {
    display: flex;
    margin-bottom: 40rpx;
    border-bottom: 1rpx solid #e8e8e8;

    .tab {
      flex: 1;
      text-align: center;
      padding: 20rpx 0;
      font-size: 30rpx;
      color: #666;

      &.active {
        color: #1890ff;
        border-bottom: 4rpx solid #1890ff;
        font-weight: bold;
      }
    }
  }

  .form-item {
    margin-bottom: 32rpx;

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

  .register-link {
    text-align: center;
    color: #1890ff;
    font-size: 28rpx;
    margin-top: 24rpx;
  }
}
</style>
