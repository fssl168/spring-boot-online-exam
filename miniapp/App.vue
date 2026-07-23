<script>
import { checkAuth } from './utils/auth'

export default {
  onLaunch () {
    // 检查登录状态，token 过期则清除
    const token = uni.getStorageSync('access_token')
    if (token) {
      // token 存在，尝试加载用户信息（失效时由 request 统一处理）
      this.$store.dispatch('user/refreshUserInfo')
    }
  },
  onShow () {
    // 小程序从后台切到前台时，检查登录状态
    checkAuth()
  }
}
</script>

<style>
/* 全局样式 */
page {
  background-color: #f5f5f5;
  font-size: 28rpx;
  color: #333;
}

/* 通用卡片样式 */
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

/* 通用按钮样式 */
.btn-primary {
  background: #1890ff;
  color: #fff;
  border-radius: 8rpx;
  padding: 20rpx 0;
  text-align: center;
  font-size: 32rpx;
}

.btn-primary[disabled] {
  background: #a0cfff;
}

/* flex 布局工具类 */
.flex { display: flex; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-column { display: flex; flex-direction: column; }
</style>
