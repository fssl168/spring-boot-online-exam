const api = {
  Login: '/auth/login',
  Logout: '/auth/logout',
  ForgePassword: '/auth/forge-password',
  Register: '/auth/register',
  twoStepCode: '/auth/2step-code',
  SendSms: '/account/sms',
  SendSmsErr: '/account/sms_err',
  // get my info
  UserInfo: '/user/info',

  // 下面是自己的用户认证的接口
  UserRegister: '/user/register',
  UserLogin: '/user/login',

  // 考试的接口
  ExamQuestionList: '/exam/question/list',
  ExamQuestionAll: '/exam/question/all',
  // Batch 7.2.2：题目服务端分页
  ExamQuestionPage: '/exam/question/page',
  ExamQuestionUpdate: '/exam/question/update',
  ExamQuestionSelection: '/exam/question/selection',
  ExamQuestionCreate: '/exam/question/create',
  ExamList: '/exam/list',
  ExamAll: '/exam/all',
  // Batch 7.2.3：考试服务端分页
  ExamPage: '/exam/page',
  // 获取问题列表，按照单选、多选和判断进行分类
  ExamQuestionTypeList: '/exam/question/type/list',
  ExamCreate: '/exam/create',
  ExamUpdate: '/exam/update',
  ExamCardList: '/exam/card/list',
  // 获取考试详情
  ExamDetail: '/exam/detail/',
  // 获取考试详情
  QuestionDetail: '/exam/question/detail/',
  // 交卷
  FinishExam: '/exam/finish/',
  ExamRecordList: '/exam/record/list',
  recordDetail: '/exam/record/detail/',
  // 获取指定考试的所有学生考试记录（教师查看）
  ExamRecordAll: '/exam/record/all/',
  // 获取指定考试的成绩统计信息（教师查看）
  ExamScoreStat: '/exam/score/stat/',

  // 公告接口（Batch 7.3.1）
  AnnouncementList: '/announcement/list',
  AnnouncementCreate: '/announcement/create',
  AnnouncementUpdate: '/announcement/update',
  AnnouncementDelete: '/announcement/'
}
export default api
