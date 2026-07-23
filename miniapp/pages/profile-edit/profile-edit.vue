<template>
  <view class="profile-edit-page">
    <view class="form card">
      <!-- 头像 -->
      <view class="form-item avatar-item">
        <text class="label">头像</text>
        <image
          class="avatar"
          :src="form.avatar || '/static/placeholder.png'"
          mode="aspectFill"
          @click="chooseAvatar"
        />
      </view>

      <view class="form-item">
        <text class="label">昵称</text>
        <input class="input" placeholder="请输入昵称" v-model="form.nickname" />
      </view>

      <view class="form-item">
        <text class="label">个人描述</text>
        <textarea class="textarea" placeholder="请输入个人描述" v-model="form.description" />
      </view>

      <view class="form-item">
        <text class="label">邮箱</text>
        <input class="input" placeholder="请输入邮箱" v-model="form.email" />
      </view>

      <view class="form-item">
        <text class="label">手机号</text>
        <input class="input" placeholder="请输入手机号" v-model="form.phone" type="number" />
      </view>

      <button class="btn-primary" :disabled="loading" @click="handleSave">
        {{ loading ? '保存中...' : '保存' }}
      </button>
    </view>
  </view>
</template>

<script>
import { getUserInfo, updateUserInfo } from '../../api/user'

export default {
  data () {
    return {
      form: {
        nickname: '',
        avatar: '',
        description: '',
        email: '',
        phone: ''
      },
      loading: false
    }
  },
  onLoad () {
    this.loadUserInfo()
  },
  methods: {
    /**
     * 加载用户信息
     */
    async loadUserInfo () {
      try {
        const res = await getUserInfo()
        if (res.code === 0 && res.data) {
          this.form.nickname = res.data.nickname || ''
          this.form.avatar = res.data.avatar || ''
          this.form.description = res.data.description || ''
          this.form.email = res.data.email || ''
          this.form.phone = res.data.phone || ''
        }
      } catch (e) {
        // 错误已统一处理
      }
    },

    /**
     * 选择头像
     */
    chooseAvatar () {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: (res) => {
          this.form.avatar = res.tempFilePaths[0]
          // TODO: 上传到服务器获取 URL，这里简化处理直接用本地路径
        }
      })
    },

    /**
     * 表单校验
     */
    validateForm () {
      if (!this.form.nickname) {
        uni.showToast({ title: '请输入昵称', icon: 'none' })
        return false
      }
      if (this.form.email) {
        const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailReg.test(this.form.email)) {
          uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
          return false
        }
      }
      if (this.form.phone && !/^1\d{10}$/.test(this.form.phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' })
        return false
      }
      return true
    },

    /**
     * 保存
     */
    async handleSave () {
      if (!this.validateForm()) return
      this.loading = true
      try {
        await updateUserInfo(this.form)
        uni.showToast({ title: '保存成功', icon: 'success' })
        // 刷新 store 中的用户信息
        this.$store.dispatch('user/refreshUserInfo')
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
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
.profile-edit-page {
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

    .textarea {
      width: 100%;
      height: 160rpx;
      border: 1rpx solid #e8e8e8;
      border-radius: 8rpx;
      padding: 16rpx 24rpx;
      font-size: 30rpx;
      box-sizing: border-box;
    }

    &.avatar-item {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .label {
        margin-bottom: 0;
      }

      .avatar {
        width: 120rpx;
        height: 120rpx;
        border-radius: 50%;
        background: #f0f0f0;
      }
    }
  }

  .btn-primary {
    width: 100%;
    margin-top: 20rpx;
  }
}
</style>
