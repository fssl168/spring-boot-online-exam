<template>
  <view class="mine-page">
    <!-- 已登录 -->
    <view v-if="isLoggedIn">
      <!-- 用户信息头部 -->
      <view class="user-header">
        <image
          class="avatar"
          :src="userInfo.avatar || '/static/placeholder.png'"
          mode="aspectFill"
        />
        <view class="user-info">
          <text class="nickname">{{ userInfo.nickname || userInfo.username }}</text>
          <text class="role">{{ formatRole(userInfo.role) }}</text>
        </view>
      </view>

      <!-- 功能菜单 -->
      <view class="menu-list card">
        <view class="menu-item" @click="goExamRecords">
          <text class="menu-icon">📝</text>
          <text class="menu-text">考试记录</text>
          <text class="menu-arrow">></text>
        </view>
        <view class="menu-item" @click="goProfileEdit">
          <text class="menu-icon">👤</text>
          <text class="menu-text">编辑资料</text>
          <text class="menu-arrow">></text>
        </view>
        <view class="menu-item" @click="goChangePassword">
          <text class="menu-icon">🔒</text>
          <text class="menu-text">修改密码</text>
          <text class="menu-arrow">></text>
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="logout-section">
        <button class="btn-logout" @click="handleLogout">退出登录</button>
      </view>
    </view>

    <!-- 未登录 -->
    <view v-else class="not-login">
      <text class="not-login-text">请先登录</text>
      <button class="btn-primary" @click="goLogin">去登录</button>
    </view>
  </view>
</template>

<script>
import { mapGetters } from 'vuex'
import { formatRole } from '../../utils/util'

export default {
  computed: {
    ...mapGetters('user', ['isLoggedIn', 'userInfo'])
  },
  onShow () {
    if (this.isLoggedIn) {
      this.$store.dispatch('user/refreshUserInfo')
    }
  },
  methods: {
    formatRole,

    /**
     * 跳转登录页
     */
    goLogin () {
      uni.navigateTo({ url: '/pages/login/login' })
    },

    /**
     * 跳转考试记录
     */
    goExamRecords () {
      uni.navigateTo({ url: '/pages/records/records' })
    },

    /**
     * 跳转编辑资料
     */
    goProfileEdit () {
      uni.navigateTo({ url: '/pages/profile-edit/profile-edit' })
    },

    /**
     * 跳转修改密码
     */
    goChangePassword () {
      uni.navigateTo({ url: '/pages/change-password/change-password' })
    },

    /**
     * 退出登录
     */
    handleLogout () {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            this.$store.dispatch('user/logout')
            uni.showToast({ title: '已退出登录', icon: 'success' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss">
.mine-page {
  min-height: 100vh;
}

.user-header {
  display: flex;
  align-items: center;
  padding: 40rpx 32rpx;
  background: linear-gradient(135deg, #1890ff, #36cfc9);

  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    border: 4rpx solid #fff;
    background: #f0f0f0;
  }

  .user-info {
    margin-left: 24rpx;

    .nickname {
      display: block;
      font-size: 36rpx;
      font-weight: bold;
      color: #fff;
      margin-bottom: 8rpx;
    }

    .role {
      display: block;
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.8);
    }
  }
}

.menu-list {
  margin: 24rpx 16rpx;
  padding: 0;

  .menu-item {
    display: flex;
    align-items: center;
    padding: 32rpx 24rpx;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .menu-icon {
      font-size: 36rpx;
      margin-right: 20rpx;
    }

    .menu-text {
      flex: 1;
      font-size: 30rpx;
      color: #333;
    }

    .menu-arrow {
      color: #ccc;
      font-size: 32rpx;
    }
  }
}

.logout-section {
  padding: 40rpx 32rpx;

  .btn-logout {
    width: 100%;
    height: 88rpx;
    background: #fff;
    color: #ff4d4f;
    border: 1rpx solid #ff4d4f;
    border-radius: 8rpx;
    font-size: 32rpx;
  }
}

.not-login {
  text-align: center;
  padding: 200rpx 64rpx;

  .not-login-text {
    display: block;
    font-size: 32rpx;
    color: #999;
    margin-bottom: 40rpx;
  }

  .btn-primary {
    width: 100%;
  }
}
</style>
