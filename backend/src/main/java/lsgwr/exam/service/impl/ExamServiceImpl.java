/***********************************************************
 * @Description : 考试服务接口实现
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-28 08:06
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lsgwr.exam.entity.*;
import lsgwr.exam.enums.ExamStatusEnum;
import lsgwr.exam.enums.QuestionEnum;
import lsgwr.exam.enums.ResultEnum;
import lsgwr.exam.enums.RoleEnum;
import lsgwr.exam.exception.ExamException;
import lsgwr.exam.service.ExamService;
import lsgwr.exam.repository.*;
import lsgwr.exam.util.AnswerParser;
import lsgwr.exam.util.AuditLogger;
import lsgwr.exam.util.IdListBuilder;
import lsgwr.exam.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.Calendar;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamServiceImpl implements ExamService {
    private static final Logger log = LoggerFactory.getLogger(ExamServiceImpl.class);

    private final ExamRepository examRepository;

    private final ExamRecordRepository examRecordRepository;

    private final QuestionRepository questionRepository;

    private final UserRepository userRepository;

    private final QuestionLevelRepository questionLevelRepository;

    private final QuestionTypeRepository questionTypeRepository;

    private final QuestionCategoryRepository questionCategoryRepository;

    private final QuestionOptionRepository questionOptionRepository;

    public ExamServiceImpl(QuestionRepository questionRepository, UserRepository userRepository, QuestionLevelRepository questionLevelRepository, QuestionTypeRepository questionTypeRepository, QuestionCategoryRepository questionCategoryRepository, QuestionOptionRepository questionOptionRepository, ExamRepository examRepository, ExamRecordRepository examRecordRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.questionLevelRepository = questionLevelRepository;
        this.questionTypeRepository = questionTypeRepository;
        this.questionCategoryRepository = questionCategoryRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.examRepository = examRepository;
        this.examRecordRepository = examRecordRepository;
    }

    @Override
    public List<QuestionVo> getQuestionAll() {
        // B-09 修复：使用 SQL 过滤软删除，与 getQuestionPage 策略一致
        // P-04 修复：限制最大返回 1000 条，避免数据量大时 OOM
        List<Question> questionList = questionRepository.findVisibleAll();
        if (questionList.size() > 1000) {
            questionList = questionList.subList(0, 1000);
        }
        return getQuestionVos(questionList);
    }

    /**
     * Batch 7.2.2：分页查询题目列表
     * 直接调用 Repository 的 findVisiblePage，软删除过滤由 SQL 完成（避免内存过滤造成的分页偏差）
     * 复用 getQuestionVos() 进行 N+1 优化后的 Vo 组装
     */
    @Override
    public PageResultVo<QuestionVo> getQuestionPage(Pageable pageable) {
        Page<Question> page = questionRepository.findVisiblePage(pageable);
        List<QuestionVo> rows = getQuestionVos(page.getContent());
        return new PageResultVo<>(rows, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    private List<QuestionVo> getQuestionVos(List<Question> questionList) {
        // Batch 5.3.2 N+1 查询优化：批量预加载所有关联实体，避免循环中逐条 findById
        if (questionList.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 收集所有需要查询的 ID 集合
        Set<String> creatorIds = new HashSet<>();
        Set<Integer> levelIds = new HashSet<>();
        Set<Integer> typeIds = new HashSet<>();
        Set<Integer> categoryIds = new HashSet<>();
        Set<String> optionIds = new HashSet<>();
        Set<String> answerOptionIds = new HashSet<>();
        for (Question q : questionList) {
            if (q.getQuestionCreatorId() != null) creatorIds.add(q.getQuestionCreatorId());
            if (q.getQuestionLevelId() != null) levelIds.add(q.getQuestionLevelId());
            if (q.getQuestionTypeId() != null) typeIds.add(q.getQuestionTypeId());
            if (q.getQuestionCategoryId() != null) categoryIds.add(q.getQuestionCategoryId());
            splitIdStr(q.getQuestionOptionIds(), optionIds);
            splitIdStr(q.getQuestionAnswerOptionIds(), answerOptionIds);
        }

        // 2. 一次性批量查询，构建 id -> 实体 的 Map
        Map<String, User> userMap = toMap(userRepository.findAllById(creatorIds), User::getUserId);
        Map<Integer, QuestionLevel> levelMap = toMap(questionLevelRepository.findAllById(levelIds), QuestionLevel::getQuestionLevelId);
        Map<Integer, QuestionType> typeMap = toMap(questionTypeRepository.findAllById(typeIds), QuestionType::getQuestionTypeId);
        Map<Integer, QuestionCategory> categoryMap = toMap(questionCategoryRepository.findAllById(categoryIds), QuestionCategory::getQuestionCategoryId);
        // 选项：把 optionIds 和 answerOptionIds 合并后一次查询
        Set<String> allOptionIds = new HashSet<>(optionIds);
        allOptionIds.addAll(answerOptionIds);
        Map<String, QuestionOption> optionMap = toMap(questionOptionRepository.findAllById(allOptionIds), QuestionOption::getQuestionOptionId);

        // 3. 循环组装 Vo，所有关联查询均从 Map 取，零额外 SQL
        List<QuestionVo> questionVoList = new ArrayList<>(questionList.size());
        for (Question question : questionList) {
            QuestionVo questionVo = new QuestionVo();
            BeanUtils.copyProperties(question, questionVo);

            User creator = userMap.get(question.getQuestionCreatorId());
            questionVo.setQuestionCreator(creator != null ? creator.getUserUsername() : null);

            QuestionLevel level = levelMap.get(question.getQuestionLevelId());
            questionVo.setQuestionLevel(level != null ? level.getQuestionLevelDescription() : null);

            QuestionType type = typeMap.get(question.getQuestionTypeId());
            questionVo.setQuestionType(type != null ? type.getQuestionTypeDescription() : null);

            QuestionCategory category = categoryMap.get(question.getQuestionCategoryId());
            questionVo.setQuestionCategory(category != null ? category.getQuestionCategoryName() : null);

            // 组装选项列表
            List<QuestionOptionVo> optionVoList = new ArrayList<>();
            List<String> qOptionIds = splitIdStrToList(question.getQuestionOptionIds());
            Set<String> qAnswerIds = new HashSet<>(splitIdStrToList(question.getQuestionAnswerOptionIds()));
            for (String optId : qOptionIds) {
                QuestionOption option = optionMap.get(optId);
                if (option == null) continue;
                QuestionOptionVo optionVo = new QuestionOptionVo();
                BeanUtils.copyProperties(option, optionVo);
                if (qAnswerIds.contains(optId)) {
                    optionVo.setAnswer(true);
                }
                optionVoList.add(optionVo);
            }
            questionVo.setQuestionOptionVoList(optionVoList);
            questionVoList.add(questionVo);
        }
        return questionVoList;
    }

    /**
     * Batch 5.3.2 辅助：将字符串中以 "-" 分隔的 ID 拆分到 Set
     * Batch 5.3.1：委托给 IdListBuilder.splitToSet
     */
    private void splitIdStr(String idStr, Set<String> target) {
        target.addAll(IdListBuilder.splitToSet(idStr));
    }

    /**
     * Batch 5.3.2 辅助：将字符串中以 "-" 分隔的 ID 拆分为 List（保留顺序）
     * Batch 5.3.1：委托给 IdListBuilder.splitToList
     */
    private List<String> splitIdStrToList(String idStr) {
        return IdListBuilder.splitToList(idStr);
    }

    /**
     * Batch 5.3.2 辅助：将 Iterable 转为以 keyExtractor 提取的 key 为键的 Map
     */
    private <K, V> Map<K, V> toMap(Iterable<V> iterable, java.util.function.Function<V, K> keyExtractor) {
        Map<K, V> map = new HashMap<>();
        if (iterable == null) return map;
        for (V v : iterable) {
            if (v != null) map.put(keyExtractor.apply(v), v);
        }
        return map;
    }

    private QuestionVo getQuestionVo(Question question) {
        QuestionVo questionVo = new QuestionVo();
        // 先复制能复制的属性
        BeanUtils.copyProperties(question, questionVo);
        // 设置问题的创建者
        questionVo.setQuestionCreator(
                Objects.requireNonNull(
                        userRepository.findById(
                                question.getQuestionCreatorId()
                        ).orElse(null)
                ).getUserUsername());

        // 设置问题的难度
        questionVo.setQuestionLevel(
                Objects.requireNonNull(
                        questionLevelRepository.findById(
                                question.getQuestionLevelId()
                        ).orElse(null)
                ).getQuestionLevelDescription());

        // 设置题目的类别，比如单选、多选、判断等
        questionVo.setQuestionType(
                Objects.requireNonNull(
                        questionTypeRepository.findById(
                                question.getQuestionTypeId()
                        ).orElse(null)
                ).getQuestionTypeDescription());

        // 设置题目分类，比如数学、语文、英语、生活、人文等
        questionVo.setQuestionCategory(
                Objects.requireNonNull(
                        questionCategoryRepository.findById(
                                question.getQuestionCategoryId()
                        ).orElse(null)
                ).getQuestionCategoryName()
        );

        // 选项的自定义Vo列表
        List<QuestionOptionVo> optionVoList = new ArrayList<>();

        // 获得所有的选项列表
        List<QuestionOption> optionList = questionOptionRepository.findAllById(
                Arrays.asList(question.getQuestionOptionIds().split("-"))
        );

        // 获取所有的答案列表optionList中每个option的isAnswer选项
        List<QuestionOption> answerList = questionOptionRepository.findAllById(
                Arrays.asList(question.getQuestionAnswerOptionIds().split("-"))
        );

        // 根据选项和答案的id相同设置optionVo的isAnswer属性
        for (QuestionOption option : optionList) {
            QuestionOptionVo optionVo = new QuestionOptionVo();
            BeanUtils.copyProperties(option, optionVo);
            for (QuestionOption answer : answerList) {
                if (option.getQuestionOptionId().equals(answer.getQuestionOptionId())) {
                    optionVo.setAnswer(true);
                }
            }
            optionVoList.add(optionVo);
        }

        // 设置题目的所有选项
        questionVo.setQuestionOptionVoList(optionVoList);
        return questionVo;
    }

    @Override
    public QuestionVo updateQuestion(QuestionVo questionVo, String userId, Integer roleId) {
        // 1.把需要的属性都设置好
        StringBuilder questionAnswerOptionIds = new StringBuilder();
        List<QuestionOption> questionOptionList = new ArrayList<>();
        List<QuestionOptionVo> questionOptionVoList = questionVo.getQuestionOptionVoList();
        int size = questionOptionVoList.size();
        for (int i = 0; i < questionOptionVoList.size(); i++) {
            QuestionOptionVo questionOptionVo = questionOptionVoList.get(i);
            QuestionOption questionOption = new QuestionOption();
            BeanUtils.copyProperties(questionOptionVo, questionOption);
            questionOptionList.add(questionOption);
            if (questionOptionVo.getAnswer()) {
                if (i != size - 1) {
                    // 把更新后的答案的id加上去,记得用-连到一起
                    questionAnswerOptionIds.append(questionOptionVo.getQuestionOptionId()).append("-");
                } else {
                    // 最后一个不需要用-连接
                    questionAnswerOptionIds.append(questionOptionVo.getQuestionOptionId());
                }
            }
        }

        // 1.更新问题
        // P0-4 修复 [I-06, A-02]：查询原题目并校验所有权，ADMIN 可操作所有
        Question question = checkQuestionOwnership(questionVo.getQuestionId(), userId, roleId);
        // P0-5 修复：保留原创建者和创建时间（QuestionVo 无 questionCreatorId/createTime 字段，BeanUtils 不会覆盖）
        String originalCreatorId = question.getQuestionCreatorId();
        Date originalCreateTime = question.getCreateTime();
        BeanUtils.copyProperties(questionVo, question);
        question.setQuestionCreatorId(originalCreatorId);
        question.setCreateTime(originalCreateTime);
        question.setQuestionAnswerOptionIds(questionAnswerOptionIds.toString());
        questionRepository.save(question);

        // 2.更新所有的option
        questionOptionRepository.saveAll(questionOptionList);

        // 返回更新后的问题，方便前端局部刷新
        return getQuestionVo(question);
    }

    @Override
    public void questionCreate(QuestionCreateVo questionCreateVo) {
        // 问题创建
        Question question = new Question();
        // 把能复制的属性都复制过来
        BeanUtils.copyProperties(questionCreateVo, question);
        // 设置下questionOptionIds和questionAnswerOptionIds，需要自己用Hutool生成下
        List<QuestionOption> questionOptionList = new ArrayList<>();
        List<QuestionOptionCreateVo> questionOptionCreateVoList = questionCreateVo.getQuestionOptionCreateVoList();
        for (QuestionOptionCreateVo questionOptionCreateVo : questionOptionCreateVoList) {
            QuestionOption questionOption = new QuestionOption();
            // 设置选项的的内容
            questionOption.setQuestionOptionContent(questionOptionCreateVo.getQuestionOptionContent());
            // 设置选项的id
            questionOption.setQuestionOptionId(IdUtil.simpleUUID());
            questionOptionList.add(questionOption);
        }
        // 把选项都存起来，然后才能用于下面设置Question的questionOptionIds和questionAnswerOptionIds
        questionOptionRepository.saveAll(questionOptionList);
        String questionOptionIds = "";
        String questionAnswerOptionIds = "";
        // 经过上面的saveAll方法，所有的option的主键id都已经持久化了
        for (int i = 0; i < questionOptionCreateVoList.size(); i++) {
            // 获取指定选项
            QuestionOptionCreateVo questionOptionCreateVo = questionOptionCreateVoList.get(i);
            // 获取保存后的指定对象
            QuestionOption questionOption = questionOptionList.get(i);
            questionOptionIds += questionOption.getQuestionOptionId() + "-";
            if (questionOptionCreateVo.getAnswer()) {
                // 如果是答案的话
                questionAnswerOptionIds += questionOption.getQuestionOptionId() + "-";
            }
        }
        // 把字符串最后面的"-"给去掉
        questionAnswerOptionIds = replaceLastSeparator(questionAnswerOptionIds);
        questionOptionIds = replaceLastSeparator(questionOptionIds);
        // 设置选项id组成的字符串
        question.setQuestionOptionIds(questionOptionIds);
        // 设置答案选项id组成的字符串
        question.setQuestionAnswerOptionIds(questionAnswerOptionIds);
        // 自己生成问题的id
        question.setQuestionId(IdUtil.simpleUUID());
        // 先把创建时间和更新时间每次都取当前时间吧
        question.setCreateTime(new Date());
        question.setUpdateTime(new Date());
        // 保存问题到数据库
        questionRepository.save(question);
    }

    @Override
    public QuestionSelectionVo getSelections() {
        QuestionSelectionVo questionSelectionVo = new QuestionSelectionVo();
        questionSelectionVo.setQuestionCategoryList(questionCategoryRepository.findAll());
        questionSelectionVo.setQuestionLevelList(questionLevelRepository.findAll());
        questionSelectionVo.setQuestionTypeList(questionTypeRepository.findAll());

        return questionSelectionVo;
    }

    /**
     * 去除字符串最后的，防止split的时候出错
     *
     * @param str 原始字符串
     * @return
     */
    public static String trimMiddleLine(String str) {
        // Batch 5.3.1：委托给 IdListBuilder（仅移除 "-"）
        return StrUtil.removeSuffix(str, IdListBuilder.DEFAULT_SEP);
    }

    @Override
    public QuestionDetailVo getQuestionDetail(String id) {
        Question question = questionRepository.findById(id).orElse(null);
        // E-02 修复：加 null 检查，避免 NPE
        if (question == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "题目不存在");
        }
        QuestionDetailVo questionDetailVo = new QuestionDetailVo();
        questionDetailVo.setId(id);
        questionDetailVo.setName(question.getQuestionName());
        questionDetailVo.setDescription(question.getQuestionDescription());
        // 问题类型，单选题/多选题/判断题
        // E-01 修复：用 null 检查替代 Objects.requireNonNull，避免 NPE
        QuestionType questionType = questionTypeRepository.findById(question.getQuestionTypeId()).orElse(null);
        if (questionType == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "题目类型不存在");
        }
        questionDetailVo.setType(questionType.getQuestionTypeDescription());
        // 获取当前问题的选项
        String optionIdsStr = trimMiddleLine(question.getQuestionOptionIds());
        String[] optionIds = optionIdsStr.split("-");
        // 获取选项列表
        List<QuestionOption> optionList = questionOptionRepository.findAllById(Arrays.asList(optionIds));
        questionDetailVo.setOptions(optionList);
        return questionDetailVo;
    }

    @Override
    public List<ExamVo> getExamAll() {
        // B-09 修复：使用 SQL 过滤软删除，与 getExamPage 策略一致
        // P-04 修复：限制最大返回 1000 条
        List<Exam> examList = examRepository.findVisibleAll();
        if (examList.size() > 1000) {
            examList = examList.subList(0, 1000);
        }
        return getExamVos(examList);
    }

    /**
     * Batch 7.2.3：分页查询考试列表
     * 软删除过滤由 SQL 完成，避免内存过滤造成的分页偏差
     * 复用 getExamVos() 进行 N+1 优化后的 Vo 组装
     */
    @Override
    public PageResultVo<ExamVo> getExamPage(Pageable pageable) {
        Page<Exam> page = examRepository.findVisiblePage(pageable);
        List<ExamVo> rows = getExamVos(page.getContent());
        return new PageResultVo<>(rows, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    private List<ExamVo> getExamVos(List<Exam> examList) {
        // Batch 5.3.3 N+1 查询优化：批量预加载创建者和题目，避免每场考试 4 次查询
        if (examList.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 收集创建者 ID 和所有题目 ID（radio / check / judge 三类合并）
        Set<String> creatorIds = new HashSet<>();
        Set<String> allQuestionIds = new HashSet<>();
        for (Exam e : examList) {
            if (e.getExamCreatorId() != null) creatorIds.add(e.getExamCreatorId());
            splitIdStr(e.getExamQuestionIdsRadio(), allQuestionIds);
            splitIdStr(e.getExamQuestionIdsCheck(), allQuestionIds);
            splitIdStr(e.getExamQuestionIdsJudge(), allQuestionIds);
        }

        // 2. 一次性批量查询
        Map<String, User> userMap = toMap(userRepository.findAllById(creatorIds), User::getUserId);
        Map<String, Question> questionMap = toMap(questionRepository.findAllById(allQuestionIds), Question::getQuestionId);

        // 3. 组装 ExamVo
        List<ExamVo> examVoList = new ArrayList<>(examList.size());
        for (Exam exam : examList) {
            ExamVo examVo = new ExamVo();
            BeanUtils.copyProperties(exam, examVo);

            User creator = userMap.get(exam.getExamCreatorId());
            examVo.setExamCreator(creator != null ? creator.getUserUsername() : null);

            // 单选题列表
            List<ExamQuestionSelectVo> radioVoList = new ArrayList<>();
            for (String qId : splitIdStrToList(exam.getExamQuestionIdsRadio())) {
                Question q = questionMap.get(qId);
                if (q == null) continue;
                ExamQuestionSelectVo vo = new ExamQuestionSelectVo();
                BeanUtils.copyProperties(q, vo);
                vo.setChecked(true);
                radioVoList.add(vo);
            }
            examVo.setExamQuestionSelectVoRadioList(radioVoList);

            // 多选题列表
            List<ExamQuestionSelectVo> checkVoList = new ArrayList<>();
            for (String qId : splitIdStrToList(exam.getExamQuestionIdsCheck())) {
                Question q = questionMap.get(qId);
                if (q == null) continue;
                ExamQuestionSelectVo vo = new ExamQuestionSelectVo();
                BeanUtils.copyProperties(q, vo);
                vo.setChecked(true);
                checkVoList.add(vo);
            }
            examVo.setExamQuestionSelectVoCheckList(checkVoList);

            // 判断题列表
            List<ExamQuestionSelectVo> judgeVoList = new ArrayList<>();
            for (String qId : splitIdStrToList(exam.getExamQuestionIdsJudge())) {
                Question q = questionMap.get(qId);
                if (q == null) continue;
                ExamQuestionSelectVo vo = new ExamQuestionSelectVo();
                BeanUtils.copyProperties(q, vo);
                vo.setChecked(true);
                judgeVoList.add(vo);
            }
            examVo.setExamQuestionSelectVoJudgeList(judgeVoList);

            examVoList.add(examVo);
        }
        return examVoList;
    }



    @Override
    public ExamQuestionTypeVo getExamQuestionType() {
        ExamQuestionTypeVo examQuestionTypeVo = new ExamQuestionTypeVo();
        // P-04 修复：每种题型限制最大返回 500 条，避免数据量大时 OOM
        Pageable limit = PageRequest.of(0, 500);
        // 获取所有单选题列表，并赋值到ExamVo的属性ExamQuestionSelectVoRadioList上
        List<ExamQuestionSelectVo> radioQuestionVoList = new ArrayList<>();
        List<Question> radioQuestionList = questionRepository.findByQuestionTypeId(QuestionEnum.RADIO.getId(), limit);
        for (Question question : radioQuestionList) {
            ExamQuestionSelectVo radioQuestionVo = new ExamQuestionSelectVo();
            BeanUtils.copyProperties(question, radioQuestionVo);
            radioQuestionVoList.add(radioQuestionVo);
        }
        examQuestionTypeVo.setExamQuestionSelectVoRadioList(radioQuestionVoList);

        // 获取所有多选题列表，并赋值到ExamVo的属性ExamQuestionSelectVoCheckList上
        List<ExamQuestionSelectVo> checkQuestionVoList = new ArrayList<>();
        List<Question> checkQuestionList = questionRepository.findByQuestionTypeId(QuestionEnum.CHECK.getId(), limit);
        for (Question question : checkQuestionList) {
            ExamQuestionSelectVo checkQuestionVo = new ExamQuestionSelectVo();
            BeanUtils.copyProperties(question, checkQuestionVo);
            checkQuestionVoList.add(checkQuestionVo);
        }
        examQuestionTypeVo.setExamQuestionSelectVoCheckList(checkQuestionVoList);

        // 获取所有多选题列表，并赋值到ExamVo的属性ExamQuestionSelectVoJudgeList上
        List<ExamQuestionSelectVo> judgeQuestionVoList = new ArrayList<>();
        List<Question> judgeQuestionList = questionRepository.findByQuestionTypeId(QuestionEnum.JUDGE.getId(), limit);
        for (Question question : judgeQuestionList) {
            ExamQuestionSelectVo judgeQuestionVo = new ExamQuestionSelectVo();
            BeanUtils.copyProperties(question, judgeQuestionVo);
            judgeQuestionVoList.add(judgeQuestionVo);
        }
        examQuestionTypeVo.setExamQuestionSelectVoJudgeList(judgeQuestionVoList);
        return examQuestionTypeVo;
    }

    @Override
    public Exam create(ExamCreateVo examCreateVo, String userId) {
        // 在线考试系统创建
        Exam exam = new Exam();
        BeanUtils.copyProperties(examCreateVo, exam);
        exam.setExamId(IdUtil.simpleUUID());
        exam.setExamCreatorId(userId);
        exam.setCreateTime(new Date());
        exam.setUpdateTime(new Date());
        // 使用前端传入的有效期，未传则默认当天有效
        if (examCreateVo.getExamStartDate() != null) {
            exam.setExamStartDate(examCreateVo.getExamStartDate());
        } else {
            exam.setExamStartDate(new Date());
        }
        if (examCreateVo.getExamEndDate() != null) {
            exam.setExamEndDate(examCreateVo.getExamEndDate());
        } else {
            // 默认7天后结束
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            exam.setExamEndDate(calendar.getTime());
        }
        String radioIdsStr = "";
        String checkIdsStr = "";
        String judgeIdsStr = "";
        List<ExamQuestionSelectVo> radios = examCreateVo.getRadios();
        List<ExamQuestionSelectVo> checks = examCreateVo.getChecks();
        List<ExamQuestionSelectVo> judges = examCreateVo.getJudges();
        int radioCnt = 0, checkCnt = 0, judgeCnt = 0;
        for (ExamQuestionSelectVo radio : radios) {
            if (radio.getChecked()) {
                radioIdsStr += radio.getQuestionId() + "-";
                radioCnt++;
            }
        }
        radioIdsStr = replaceLastSeparator(radioIdsStr);
        for (ExamQuestionSelectVo check : checks) {
            if (check.getChecked()) {
                checkIdsStr += check.getQuestionId() + "-";
                checkCnt++;
            }
        }
        checkIdsStr = replaceLastSeparator(checkIdsStr);
        for (ExamQuestionSelectVo judge : judges) {
            if (judge.getChecked()) {
                judgeIdsStr += judge.getQuestionId() + "-";
                judgeCnt++;
            }
        }
        judgeIdsStr = replaceLastSeparator(judgeIdsStr);
        exam.setExamQuestionIds(radioIdsStr + "-" + checkIdsStr + "-" + judgeIdsStr);
        // 设置各个题目的id
        exam.setExamQuestionIdsRadio(radioIdsStr);
        exam.setExamQuestionIdsCheck(checkIdsStr);
        exam.setExamQuestionIdsJudge(judgeIdsStr);

        // 计算总分数
        int examScore = radioCnt * exam.getExamScoreRadio() + checkCnt * exam.getExamScoreCheck() + judgeCnt * exam.getExamScoreJudge();
        exam.setExamScore(examScore);
        examRepository.save(exam);
        // P-08 修复：审计日志 - 创建考试
        AuditLogger.success(userId, "EXAM_CREATE", exam.getExamId(),
                "name=" + exam.getExamName() + ",score=" + examScore);
        return exam;
    }

    @Override
    public Exam update(ExamVo examVo, String userId, Integer roleId) {
        // P0-4 修复 [I-05, A-02]：查询原考试并校验所有权，ADMIN 可操作所有
        Exam exam = checkExamOwnership(examVo.getExamId(), userId, roleId);
        // P0-5 修复 [B-12]：保留原创建者和创建时间，仅更新 updateTime
        String originalCreatorId = exam.getExamCreatorId();
        Date originalCreateTime = exam.getCreateTime();
        BeanUtils.copyProperties(examVo, exam);
        exam.setExamCreatorId(originalCreatorId);
        exam.setCreateTime(originalCreateTime);
        exam.setUpdateTime(new Date()); // 考试的更新日期要记录下

        String radioIdsStr = "";
        String checkIdsStr = "";
        String judgeIdsStr = "";
        List<ExamQuestionSelectVo> radios = examVo.getExamQuestionSelectVoRadioList();
        List<ExamQuestionSelectVo> checks = examVo.getExamQuestionSelectVoCheckList();
        List<ExamQuestionSelectVo> judges = examVo.getExamQuestionSelectVoJudgeList();
        int radioCnt = 0, checkCnt = 0, judgeCnt = 0;
        for (ExamQuestionSelectVo radio : radios) {
            if (radio.getChecked()) {
                radioIdsStr += radio.getQuestionId() + "-";
                radioCnt++;
            }
        }
        radioIdsStr = replaceLastSeparator(radioIdsStr);
        for (ExamQuestionSelectVo check : checks) {
            if (check.getChecked()) {
                checkIdsStr += check.getQuestionId() + "-";
                checkCnt++;
            }
        }
        checkIdsStr = replaceLastSeparator(checkIdsStr);
        for (ExamQuestionSelectVo judge : judges) {
            if (judge.getChecked()) {
                judgeIdsStr += judge.getQuestionId() + "-";
                judgeCnt++;
            }
        }
        judgeIdsStr = replaceLastSeparator(judgeIdsStr);
        exam.setExamQuestionIds(radioIdsStr + "-" + checkIdsStr + "-" + judgeIdsStr);
        // 设置各个题目的id
        exam.setExamQuestionIdsRadio(radioIdsStr);
        exam.setExamQuestionIdsCheck(checkIdsStr);
        exam.setExamQuestionIdsJudge(judgeIdsStr);

        // 计算总分数
        int examScore = radioCnt * exam.getExamScoreRadio() + checkCnt * exam.getExamScoreCheck() + judgeCnt * exam.getExamScoreJudge();
        exam.setExamScore(examScore);
        examRepository.save(exam);
        // P-08 修复：审计日志 - 更新考试
        AuditLogger.success(userId, "EXAM_UPDATE", exam.getExamId(),
                "name=" + exam.getExamName() + ",score=" + examScore);
        return exam;
    }

    @Override
    public List<ExamCardVo> getExamCardList() {
        // B-09 修复：使用 SQL 过滤软删除，与 getExamPage 策略一致
        List<Exam> examList = examRepository.findVisibleAll();
        if (examList.size() > 1000) {
            examList = examList.subList(0, 1000);
        }
        List<ExamCardVo> examCardVoList = new ArrayList<>();
        for (Exam exam : examList) {
            ExamStatusEnum status = checkExamAvailable(exam);
            // 过滤掉已结束的考试：学生考试卡片列表只展示未开始 + 进行中的考试
            // 已结束的考试可通过考试记录页面查看
            if (status == ExamStatusEnum.ENDED) {
                continue;
            }
            ExamCardVo examCardVo = new ExamCardVo();
            BeanUtils.copyProperties(exam, examCardVo);
            // 设置考试状态
            examCardVo.setStatus(status.getId());
            examCardVoList.add(examCardVo);
        }
        return examCardVoList;
    }

    /**
     * 检查考试当前是否在有效期内
     *
     * @param exam 考试对象
     * @return 考试状态枚举
     */
    private ExamStatusEnum checkExamAvailable(Exam exam) {
        // S-01 修复：优先检查手动状态覆盖
        Integer manualStatus = exam.getExamManualStatus();
        if (manualStatus != null) {
            if (manualStatus == 1) {
                // 手动结束（教师提前收卷）
                return ExamStatusEnum.ENDED;
            } else if (manualStatus == 2) {
                // 手动暂停（强制不可用，视为 UPCOMING）
                return ExamStatusEnum.UPCOMING;
            }
        }
        // 默认：基于时间自动计算
        Date now = new Date();
        if (now.before(exam.getExamStartDate())) {
            return ExamStatusEnum.UPCOMING;
        } else if (now.after(exam.getExamEndDate())) {
            return ExamStatusEnum.ENDED;
        } else {
            return ExamStatusEnum.AVAILABLE;
        }
    }

    @Override
    public ExamStatusEnum checkExamAvailable(String examId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        // E-01 修复：用显式异常替代 assert
        if (exam == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试不存在");
        }
        return checkExamAvailable(exam);
    }

    @Override
    public ExamDetailVo getExamDetail(String id) {
        Exam exam = examRepository.findById(id).orElse(null);
        ExamDetailVo examDetailVo = new ExamDetailVo();
        examDetailVo.setExam(exam);
        // E-03 修复：用显式异常替代 assert，避免 exam 为 null 时 NPE
        if (exam == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试不存在");
        }
        examDetailVo.setRadioIds(exam.getExamQuestionIdsRadio().split("-"));
        examDetailVo.setCheckIds(exam.getExamQuestionIdsCheck().split("-"));
        examDetailVo.setJudgeIds(exam.getExamQuestionIdsJudge().split("-"));
        return examDetailVo;
    }

    @Override
    public ExamRecord judge(String userId, String examId, HashMap<String, List<String>> answersMap, Integer timeCost) {
        // 开始考试判分啦~~~
        // 1.首先获取考试对象和选项数组
        ExamDetailVo examDetailVo = getExamDetail(examId);
        Exam exam = examDetailVo.getExam();
        // 1.1 服务端二次校验考试有效期（防御性：Controller 已校验过，Service 再校验一次）
        ExamStatusEnum status = checkExamAvailable(exam);
        if (status == ExamStatusEnum.UPCOMING) {
            throw new ExamException(ResultEnum.EXAM_NOT_STARTED);
        } else if (status == ExamStatusEnum.ENDED) {
            throw new ExamException(ResultEnum.EXAM_ENDED);
        }
        // 1.0 P0-2 修复 [D-01, I-02]：重复提交校验，同一学生对同一考试只能交卷一次
        List<ExamRecord> existingRecords = examRecordRepository.findByExamIdAndExamJoinerId(examId, userId);
        if (!existingRecords.isEmpty()) {
            throw new ExamException(ResultEnum.ORDER_STATUS_ERR.getCode(), "您已交卷，无法重复提交");
        }
        // 1.2 服务端校验考试时长：examTimeLimit 单位为分钟，timeCost 单位为秒
        // 给予 30 秒的网络/提交延迟宽限期
        if (exam.getExamTimeLimit() != null && exam.getExamTimeLimit() > 0) {
            int limitSeconds = exam.getExamTimeLimit() * 60;
            int gracePeriod = 30;
            int actualCost = timeCost != null ? timeCost : 0;
            if (actualCost > limitSeconds + gracePeriod) {
                throw new ExamException(ResultEnum.TIME_EXPIRED);
            }
        }
        // 2.然后获取该考试下所有的题目信息
        List<String> questionIds = new ArrayList<>();
        // 2.1 题目id的数组
        List<String> radioIdList = Arrays.asList(examDetailVo.getRadioIds());
        List<String> checkIdList = Arrays.asList(examDetailVo.getCheckIds());
        List<String> judgeIdList = Arrays.asList(examDetailVo.getJudgeIds());
        questionIds.addAll(radioIdList);
        questionIds.addAll(checkIdList);
        questionIds.addAll(judgeIdList);
        // 2.2 每种题目的分数
        int radioScore = exam.getExamScoreRadio();
        int checkScore = exam.getExamScoreCheck();
        int judgeScore = exam.getExamScoreJudge();
        // 2.3 根据问题id的数组拿到所有的问题对象，供下面步骤用
        List<Question> questionList = questionRepository.findAllById(questionIds);
        Map<String, Question> questionMap = new HashMap<>();
        for (Question question : questionList) {
            questionMap.put(question.getQuestionId(), question);
        }
        // 3.根据正确答案和用户作答信息进行判分
        // P0-1 修复 [B-01]：必须遍历考试的所有题目（questionIds），而非用户提交的题目（answersMap.keySet()）
        // 否则学生故意不提交某题时，该题不会被记 0 分，导致得分虚高
        // 存储当前考试每个题目的得分情况
        Map<String, Integer> judgeMap = new HashMap<>();
        // 考生作答地每个题目的选项(题目和题目之间用$分隔，题目有多个选项地话用-分隔,题目和选项之间用_分隔),用于查看考试详情
        // 例子：题目1的id_作答选项1-作答选项2&题目2的id_作答选项1&题目3_作答选项1-作答选项2-作答选项3
        // Batch 5.3.4：使用 AnswerParser 收集作答条目，统一拼接
        List<AnswerParser.AnswerEntry> answerEntries = new ArrayList<>();
        // 用户此次考试的总分
        int totalScore = 0;
        for (String questionId : questionIds) {
            // 获取用户作答地这个题的答案信息
            Question question = questionMap.get(questionId);
            // P1 修复 [B-03, E-08]：防御性跳过非法 questionId，避免 NPE
            if (question == null) {
                continue;
            }
            // 获取答案选项（Batch 5.3.4：用 AnswerParser.optionsEquals 替代手写排序+拼接+比较）
            List<String> questionAnswerOptionIdList = IdListBuilder.splitToList(
                    replaceLastSeparator(question.getQuestionAnswerOptionIds()));
            // 获取用户作答；未作答的题目视为空列表（计 0 分）
            List<String> questionUserOptionIdList = answersMap.getOrDefault(questionId, Collections.emptyList());
            // 判断questionAnswerOptionIds和answersMap里面的答案是否相等
            boolean correct = !questionUserOptionIdList.isEmpty()
                    && AnswerParser.optionsEquals(questionAnswerOptionIdList, questionUserOptionIdList);
            if (correct) {
                // 说明题目作答正确,下面根据题型给分
                int score = 0;
                // B-04 修复：使用 else if 避免重复匹配，并检测异常 questionId
                if (radioIdList.contains(questionId)) {
                    score = radioScore;
                } else if (checkIdList.contains(questionId)) {
                    score = checkScore;
                } else if (judgeIdList.contains(questionId)) {
                    score = judgeScore;
                } else {
                    // B-04 修复：questionId 不在任何题型列表中，记录警告
                    log.warn("判分异常：题目{}不在考试{}的任何题型列表中", questionId, examId);
                }
                // 累计本次考试得分
                totalScore += score;
                judgeMap.put(questionId, score);
            } else {
                // 说明题目作答错误,直接判零分
                judgeMap.put(questionId, 0);
            }
            // 收集作答条目，最后统一拼接
            answerEntries.add(new AnswerParser.AnswerEntry(questionId, correct, questionUserOptionIdList));
        }
        // 4.计算得分，记录本次考试结果，存到ExamRecord中
        ExamRecord examRecord = new ExamRecord();
        examRecord.setExamRecordId(IdUtil.simpleUUID());
        examRecord.setExamId(examId);
        // Batch 5.3.4：使用 AnswerParser.build 统一拼接作答串，自动处理分隔符
        examRecord.setAnswerOptionIds(AnswerParser.build(answerEntries));
        examRecord.setExamJoinerId(userId);
        examRecord.setExamJoinDate(new Date());
        examRecord.setExamJoinScore(totalScore);
        // 记录考试耗时（秒），如果前端未传则默认为0
        examRecord.setExamTimeCost(timeCost != null ? timeCost : 0);
        // 记录本次考试展示给考生的题目顺序（与 getExamDetail 中的 shuffle 一致，基于 userId 确定性）
        String[] shuffledRadioIds = shuffleQuestionIds(examDetailVo.getRadioIds(), userId);
        String[] shuffledCheckIds = shuffleQuestionIds(examDetailVo.getCheckIds(), userId);
        String[] shuffledJudgeIds = shuffleQuestionIds(examDetailVo.getJudgeIds(), userId);
        StringBuilder orderSb = new StringBuilder();
        for (String qid : shuffledRadioIds) {
            orderSb.append(qid).append("-");
        }
        for (String qid : shuffledCheckIds) {
            orderSb.append(qid).append("-");
        }
        for (String qid : shuffledJudgeIds) {
            orderSb.append(qid).append("-");
        }
        examRecord.setQuestionOrder(replaceLastSeparator(orderSb.toString()));
        // P1 修复 [B-02]：根据得分率计算得分级别：0=不及格, 1=及格, 2=良, 3=优
        Integer examTotalScore = exam.getExamScore();
        if (examTotalScore != null && examTotalScore > 0) {
            double ratio = (double) totalScore / examTotalScore;
            int level;
            if (ratio >= 0.9) {
                level = 3; // 优
            } else if (ratio >= 0.75) {
                level = 2; // 良
            } else if (ratio >= 0.6) {
                level = 1; // 及格
            } else {
                level = 0; // 不及格
            }
            examRecord.setExamResultLevel(level);
        } else {
            examRecord.setExamResultLevel(0);
        }
        // S-02 修复：设置记录状态为已交卷
        examRecord.setStatus(1);
        // S-04 修复：捕获交卷时考试有效期快照，便于教师后续修改有效期后的审计追溯
        examRecord.setExamStartDateSnapshot(exam.getExamStartDate());
        examRecord.setExamEndDateSnapshot(exam.getExamEndDate());
        examRecordRepository.save(examRecord);
        // P-08 修复：审计日志 - 学生交卷
        AuditLogger.success(userId, "EXAM_SUBMIT", examId,
                "recordId=" + examRecord.getExamRecordId() + ",score=" + totalScore);
        return examRecord;
    }

    @Override
    public List<ExamRecordVo> getExamRecordList(String userId) {
        // 获取指定用户下的考试记录列表
        List<ExamRecord> examRecordList = examRecordRepository.findByExamJoinerIdOrderByExamJoinDateDesc(userId);
        if (examRecordList.isEmpty()) {
            return new ArrayList<>();
        }
        // P-01 修复：批量预加载 exam 和 user，避免循环中 N+1 查询
        Set<String> examIds = new HashSet<>();
        for (ExamRecord record : examRecordList) {
            if (record.getExamId() != null) {
                examIds.add(record.getExamId());
            }
        }
        Map<String, Exam> examMap = toMap(examRepository.findAllById(examIds), Exam::getExamId);
        User user = userRepository.findById(userId).orElse(null);
        List<ExamRecordVo> examRecordVoList = new ArrayList<>();
        for (ExamRecord examRecord : examRecordList) {
            ExamRecordVo examRecordVo = new ExamRecordVo();
            examRecordVo.setExam(examMap.get(examRecord.getExamId()));
            examRecordVo.setUser(user);
            examRecordVo.setExamRecord(examRecord);
            examRecordVoList.add(examRecordVo);
        }
        return examRecordVoList;
    }

    @Override
    public RecordDetailVo getRecordDetail(String recordId) {
        // 获取考试详情的封装对象
        ExamRecord record = examRecordRepository.findById(recordId).orElse(null);
        RecordDetailVo recordDetailVo = new RecordDetailVo();
        recordDetailVo.setExamRecord(record);
        // E-04 修复：用显式异常替代 assert，避免 record 为 null 时 NPE
        if (record == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试记录不存在");
        }
        // Batch 5.3.4：使用 AnswerParser 解析作答串，替代手写 split 逻辑
        String answersStr = record.getAnswerOptionIds();
        recordDetailVo.setAnswersMap(new HashMap<>(AnswerParser.parseUserAnswers(answersStr)));
        recordDetailVo.setResultsMap(new HashMap<>(AnswerParser.parseResults(answersStr)));
        // 下面再计算正确答案的map
        ExamDetailVo examDetailVo = getExamDetail(record.getExamId());
        List<String> questionIdList = new ArrayList<>();
        questionIdList.addAll(Arrays.asList(examDetailVo.getRadioIds()));
        questionIdList.addAll(Arrays.asList(examDetailVo.getCheckIds()));
        questionIdList.addAll(Arrays.asList(examDetailVo.getJudgeIds()));
        // 获取所有的问题对象
        List<Question> questionList = questionRepository.findAllById(questionIdList);
        HashMap<String, List<String>> answersRightMap = new HashMap<>();
        for (Question question : questionList) {
            // Batch 5.3.1：使用 IdListBuilder.splitToList 替代手写 split + replaceLastSeparator
            answersRightMap.put(question.getQuestionId(),
                    IdListBuilder.splitToList(replaceLastSeparator(question.getQuestionAnswerOptionIds())));
        }
        recordDetailVo.setAnswersRightMap(answersRightMap);
        return recordDetailVo;
    }

    /**
     * 把字符串最后一个字符-替换掉
     *
     * @param str 原始字符串
     * @return 替换掉最后一个-的字符串
     */
    private String replaceLastSeparator(String str) {
        // Batch 5.3.1：委托给 IdListBuilder.trimTrailingSep
        return IdListBuilder.trimTrailingSep(str);
    }

    /**
     * 把字符串用-连接起来
     *
     * @param strList 字符串列表
     * @return 拼接好的字符串，记住要去掉最后面的-
     * Batch 5.3.1：委托给 IdListBuilder.join
     */
    private String listConcat(List<String> strList) {
        return IdListBuilder.join(strList, IdListBuilder.DEFAULT_SEP);
    }

    @Override
    public String[] shuffleQuestionIds(String[] questionIds, String seed) {
        return shuffleArray(questionIds, seed);
    }

    /**
     * 基于种子确定性打乱数组，相同userId每次得到相同顺序，不同学生顺序不同
     *
     * @param array 待打乱的数组
     * @param seed  随机种子（通常为userId）
     * @return 打乱后的数组
     */
    private String[] shuffleArray(String[] array, String seed) {
        if (array == null || array.length <= 1) {
            return array;
        }
        String[] result = Arrays.copyOf(array, array.length);
        // 使用种子初始化随机数生成器，保证同一学生每次顺序一致
        Random random = new Random(seed.hashCode());
        for (int i = result.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String temp = result[i];
            result[i] = result[j];
            result[j] = temp;
        }
        return result;
    }

    /**
     * 获取指定考试的所有学生考试记录（教师查看）
     *
     * @param examId 考试id
     * @return 所有考试记录VO列表
     */
    @Override
    public List<ExamRecordVo> getExamAllRecords(String examId, String userId, Integer roleId) {
        // A-06 修复：校验考试所有权，ADMIN 可查看所有
        checkExamOwnership(examId, userId, roleId);
        List<ExamRecord> examRecordList = examRecordRepository.findByExamId(examId);
        // 按参加时间倒序排列
        examRecordList.sort((a, b) -> {
            if (a.getExamJoinDate() == null) return 1;
            if (b.getExamJoinDate() == null) return -1;
            return b.getExamJoinDate().compareTo(a.getExamJoinDate());
        });
        Exam exam = examRepository.findById(examId).orElse(null);
        // P-02 修复：批量预加载所有 user，避免循环中 N+1 查询
        Set<String> joinerIds = new HashSet<>();
        for (ExamRecord record : examRecordList) {
            if (record.getExamJoinerId() != null) {
                joinerIds.add(record.getExamJoinerId());
            }
        }
        Map<String, User> userMap = toMap(userRepository.findAllById(joinerIds), User::getUserId);
        List<ExamRecordVo> examRecordVoList = new ArrayList<>();
        for (ExamRecord examRecord : examRecordList) {
            ExamRecordVo examRecordVo = new ExamRecordVo();
            examRecordVo.setExam(exam);
            examRecordVo.setUser(userMap.get(examRecord.getExamJoinerId()));
            examRecordVo.setExamRecord(examRecord);
            examRecordVoList.add(examRecordVo);
        }
        return examRecordVoList;
    }

    /**
     * 获取指定考试的成绩统计信息
     * 计算参考人数、平均分、最高/最低分、及格率（默认按60%及格）和分数段分布
     *
     * @param examId 考试id
     * @return 成绩统计VO
     */
    @Override
    public ExamScoreStatVo getExamScoreStat(String examId, String userId, Integer roleId) {
        // A-06 修复：校验考试所有权，ADMIN 可查看所有
        checkExamOwnership(examId, userId, roleId);
        Exam exam = examRepository.findById(examId).orElse(null);
        ExamScoreStatVo statVo = new ExamScoreStatVo();
        statVo.setExamId(examId);
        if (exam == null) {
            statVo.setTotalCount(0);
            return statVo;
        }
        statVo.setExamName(exam.getExamName());
        statVo.setExamScore(exam.getExamScore());

        List<ExamRecord> recordList = examRecordRepository.findByExamId(examId);
        statVo.setTotalCount(recordList.size());
        if (recordList.isEmpty()) {
            statVo.setAvgScore(0.0);
            statVo.setMaxScore(0);
            statVo.setMinScore(0);
            statVo.setPassRate(0.0);
            statVo.setScoreDistribution(new LinkedHashMap<>());
            return statVo;
        }

        int sum = 0;
        int maxScore = Integer.MIN_VALUE;
        int minScore = Integer.MAX_VALUE;
        int passCount = 0;
        // 满分的60%视为及格
        int passLine = (int) Math.ceil(exam.getExamScore() * 0.6);
        // 分数段：0-59, 60-69, 70-79, 80-89, 90-100
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("0-59", 0);
        distribution.put("60-69", 0);
        distribution.put("70-79", 0);
        distribution.put("80-89", 0);
        distribution.put("90-100", 0);
        for (ExamRecord record : recordList) {
            Integer score = record.getExamJoinScore();
            if (score == null) {
                score = 0;
            }
            sum += score;
            if (score > maxScore) {
                maxScore = score;
            }
            if (score < minScore) {
                minScore = score;
            }
            if (score >= passLine) {
                passCount++;
            }
            // 归入对应分数段
            if (score < 60) {
                distribution.merge("0-59", 1, Integer::sum);
            } else if (score < 70) {
                distribution.merge("60-69", 1, Integer::sum);
            } else if (score < 80) {
                distribution.merge("70-79", 1, Integer::sum);
            } else if (score < 90) {
                distribution.merge("80-89", 1, Integer::sum);
            } else {
                distribution.merge("90-100", 1, Integer::sum);
            }
        }
        int count = recordList.size();
        statVo.setAvgScore(Math.round(sum * 100.0 / count) / 100.0);
        statVo.setMaxScore(maxScore);
        statVo.setMinScore(minScore);
        statVo.setPassRate(Math.round(passCount * 10000.0 / count) / 100.0);
        statVo.setScoreDistribution(distribution);
        return statVo;
    }

    /**
     * 软删除题目：将 questionVisible 置为 0，不物理删除
     *
     * @param questionId 题目id
     */
    @Override
    public void deleteQuestion(String questionId, String userId, Integer roleId) {
        // P0-4 修复 [I-07, A-02]：校验题目所有权，ADMIN 可操作所有
        Question question = checkQuestionOwnership(questionId, userId, roleId);
        question.setQuestionVisible(0);
        questionRepository.save(question);
        // P-08 修复：审计日志 - 删除题目
        AuditLogger.success(userId, "QUESTION_DELETE", questionId, "softDelete");
    }

    /**
     * 软删除考试：将 examVisible 置为 0，不物理删除
     *
     * @param examId 考试id
     */
    @Override
    public void deleteExam(String examId, String userId, Integer roleId) {
        // P0-4 修复 [I-07, A-02]：校验考试所有权，ADMIN 可操作所有
        Exam exam = checkExamOwnership(examId, userId, roleId);
        exam.setExamVisible(0);
        examRepository.save(exam);
        // P-08 修复：审计日志 - 删除考试
        AuditLogger.success(userId, "EXAM_DELETE", examId, "softDelete");
    }

    // ==================== P0-4 所有权校验私有方法 ====================

    /**
     * P0-4 修复：校验考试所有权
     * 查询考试是否存在，并校验当前用户是否有权操作该考试（创建者本人或 ADMIN）
     *
     * @param examId 考试id
     * @param userId 当前操作用户id
     * @param roleId 当前操作用户角色id
     * @return 查询到的考试实体
     * @throws ExamException 考试不存在或无权操作
     */
    private Exam checkExamOwnership(String examId, String userId, Integer roleId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) {
            throw new ExamException(ResultEnum.PRODUCT_STOCK_ERR.getCode(), "考试不存在");
        }
        boolean isAdmin = roleId != null && roleId.equals(RoleEnum.ADMIN.getId());
        if (!isAdmin && !userId.equals(exam.getExamCreatorId())) {
            throw new ExamException(-3, "无权操作他人考试");
        }
        return exam;
    }

    /**
     * P0-4 修复：校验题目所有权
     * 查询题目是否存在，并校验当前用户是否有权操作该题目（创建者本人或 ADMIN）
     *
     * @param questionId 题目id
     * @param userId     当前操作用户id
     * @param roleId     当前操作用户角色id
     * @return 查询到的题目实体
     * @throws ExamException 题目不存在或无权操作
     */
    private Question checkQuestionOwnership(String questionId, String userId, Integer roleId) {
        Question question = questionRepository.findById(questionId).orElse(null);
        if (question == null) {
            throw new ExamException(ResultEnum.PRODUCT_NOT_EXIST.getCode(), "题目不存在");
        }
        boolean isAdmin = roleId != null && roleId.equals(RoleEnum.ADMIN.getId());
        if (!isAdmin && !userId.equals(question.getQuestionCreatorId())) {
            throw new ExamException(-3, "无权操作他人题目");
        }
        return question;
    }

    // ==================== Batch 3.1 随机组卷 ====================

    /**
     * 随机组卷：按题型数量从题库随机抽题，自动组装考试
     */
    @Override
    public Exam randomExamCreate(RandomExamCreateVo randomVo, String userId) {
        // 获取各题型的可见题目
        List<Question> radioPool = getVisibleQuestionsByType(1); // 单选
        List<Question> checkPool = getVisibleQuestionsByType(2); // 多选
        List<Question> judgePool = getVisibleQuestionsByType(3); // 判断

        // 随机抽取指定数量
        List<String> radioIds = randomPick(radioPool, randomVo.getRadioCount());
        List<String> checkIds = randomPick(checkPool, randomVo.getCheckCount());
        List<String> judgeIds = randomPick(judgePool, randomVo.getJudgeCount());

        // 校验题库是否充足
        if (radioIds.size() < randomVo.getRadioCount() || checkIds.size() < randomVo.getCheckCount()
                || judgeIds.size() < randomVo.getJudgeCount()) {
            throw new ExamException(ResultEnum.PRODUCT_STOCK_ERR.getCode(), "题库题目数量不足，无法完成随机组卷");
        }

        // 组装 ExamCreateVo 并复用 create() 逻辑
        ExamCreateVo examCreateVo = new ExamCreateVo();
        examCreateVo.setExamName(randomVo.getExamName());
        examCreateVo.setExamAvatar(randomVo.getExamAvatar());
        examCreateVo.setExamDescription(randomVo.getExamDescription());
        examCreateVo.setExamTimeLimit(randomVo.getExamTimeLimit());
        examCreateVo.setExamStartDate(randomVo.getExamStartDate());
        examCreateVo.setExamEndDate(randomVo.getExamEndDate());
        examCreateVo.setExamScoreRadio(randomVo.getExamScoreRadio());
        examCreateVo.setExamScoreCheck(randomVo.getExamScoreCheck());
        examCreateVo.setExamScoreJudge(randomVo.getExamScoreJudge());

        // 构建 ExamQuestionSelectVo 列表（checked=true 表示选中）
        List<ExamQuestionSelectVo> radios = new ArrayList<>();
        for (String qid : radioIds) {
            ExamQuestionSelectVo vo = new ExamQuestionSelectVo();
            vo.setQuestionId(qid);
            vo.setChecked(true);
            radios.add(vo);
        }
        List<ExamQuestionSelectVo> checks = new ArrayList<>();
        for (String qid : checkIds) {
            ExamQuestionSelectVo vo = new ExamQuestionSelectVo();
            vo.setQuestionId(qid);
            vo.setChecked(true);
            checks.add(vo);
        }
        List<ExamQuestionSelectVo> judges = new ArrayList<>();
        for (String qid : judgeIds) {
            ExamQuestionSelectVo vo = new ExamQuestionSelectVo();
            vo.setQuestionId(qid);
            vo.setChecked(true);
            judges.add(vo);
        }
        examCreateVo.setRadios(radios);
        examCreateVo.setChecks(checks);
        examCreateVo.setJudges(judges);

        return create(examCreateVo, userId);
    }

    /**
     * 获取指定题型的可见题目列表
     */
    private List<Question> getVisibleQuestionsByType(Integer typeId) {
        return questionRepository.findByQuestionTypeId(typeId).stream()
                .filter(q -> q.getQuestionVisible() == null || q.getQuestionVisible() == 1)
                .collect(Collectors.toList());
    }

    /**
     * 从题目池中随机抽取 n 道题的 id
     */
    private List<String> randomPick(List<Question> pool, int count) {
        List<String> result = new ArrayList<>();
        if (pool.isEmpty() || count <= 0) {
            return result;
        }
        // B-07 修复：使用基于时间戳的种子进行 shuffle，保留随机性同时可审计
        long seed = System.currentTimeMillis();
        Collections.shuffle(pool, new java.util.Random(seed));
        log.debug("randomPick: seed={}, poolSize={}, pick={}", seed, pool.size(), count);
        int actual = Math.min(count, pool.size());
        for (int i = 0; i < actual; i++) {
            result.add(pool.get(i).getQuestionId());
        }
        return result;
    }

    // ==================== Batch 3.2 班级排名 ====================

    /**
     * 班级排名：按考试分组统计学生成绩排名
     */
    @Override
    public List<ClassRankingVo> getClassRanking(String examId, String userId, Integer roleId) {
        // A-06 修复：校验考试所有权，ADMIN 可查看所有
        checkExamOwnership(examId, userId, roleId);
        List<ExamRecord> recordList = examRecordRepository.findByExamId(examId);
        // 按分数降序排序，同分按耗时升序
        recordList.sort((a, b) -> {
            int sa = a.getExamJoinScore() != null ? a.getExamJoinScore() : 0;
            int sb = b.getExamJoinScore() != null ? b.getExamJoinScore() : 0;
            if (sb != sa) {
                return sb - sa;
            }
            int ta = a.getExamTimeCost() != null ? a.getExamTimeCost() : 0;
            int tb = b.getExamTimeCost() != null ? b.getExamTimeCost() : 0;
            return ta - tb;
        });
        // P-03 修复：批量预加载所有 user，避免循环中 N+1 查询
        Set<String> joinerIds = new HashSet<>();
        for (ExamRecord record : recordList) {
            if (record.getExamJoinerId() != null) {
                joinerIds.add(record.getExamJoinerId());
            }
        }
        Map<String, User> userMap = toMap(userRepository.findAllById(joinerIds), User::getUserId);
        List<ClassRankingVo> rankingList = new ArrayList<>();
        int rank = 1;
        for (ExamRecord record : recordList) {
            ClassRankingVo vo = new ClassRankingVo();
            vo.setRank(rank++);
            vo.setUserId(record.getExamJoinerId());
            vo.setScore(record.getExamJoinScore() != null ? record.getExamJoinScore() : 0);
            vo.setTimeCost(record.getExamTimeCost());
            vo.setJoinDate(record.getExamJoinDate() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getExamJoinDate())
                    : "");
            // 从批量预加载的 Map 中获取学生昵称
            User user = userMap.get(record.getExamJoinerId());
            vo.setNickname(user != null ? user.getUserNickname() : "未知");
            rankingList.add(vo);
        }
        return rankingList;
    }

    // ==================== Batch 3.3 主观题批改 ====================

    /**
     * 教师批改主观题：提交主观题评分
     */
    @Override
    public void gradeEssay(String recordId, String questionId, Integer score, String userId, Integer roleId) {
        ExamRecord record = examRecordRepository.findById(recordId).orElse(null);
        if (record == null) {
            throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试记录不存在");
        }
        // A-05 修复：校验考试记录所属考试为本教师创建，ADMIN 可批改所有
        checkExamOwnership(record.getExamId(), userId, roleId);
        // 更新 essayScores 字段，格式：questionId:score,questionId:score
        String essayScores = record.getEssayScores();
        if (essayScores == null || essayScores.isEmpty()) {
            essayScores = "";
        }
        // B-05 修复：提取旧评分用于后续重新计算总分
        int oldEssayScore = 0;
        // 移除已有的该题评分（防止重复评分）
        StringBuilder sb = new StringBuilder();
        for (String pair : essayScores.split(",")) {
            if (!pair.trim().isEmpty() && !pair.startsWith(questionId + ":")) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(pair.trim());
            } else if (pair.trim().startsWith(questionId + ":")) {
                try {
                    oldEssayScore = Integer.parseInt(pair.trim().substring(pair.trim().indexOf(":") + 1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (sb.length() > 0) {
            sb.append(",");
        }
        sb.append(questionId).append(":").append(score);
        record.setEssayScores(sb.toString());
        // B-05 修复：重新计算 examJoinScore = 原总分 - 旧主观题分 + 新主观题分
        int currentTotal = record.getExamJoinScore() != null ? record.getExamJoinScore() : 0;
        int newTotal = currentTotal - oldEssayScore + score;
        record.setExamJoinScore(Math.max(0, newTotal));
        // S-02 修复：设置记录状态为已批改
        record.setStatus(2);
        examRecordRepository.save(record);
        log.info("教师批改主观题：记录{}题目{}评分{}，总分更新为{}", recordId, questionId, score, newTotal);
        // P-08 修复：审计日志 - 主观题批改
        AuditLogger.success(userId, "ESSAY_GRADE", recordId,
                "questionId=" + questionId + ",score=" + score + ",newTotal=" + newTotal);
    }

    // ==================== Batch 3.4 批量导入题目 ====================

    /**
     * 批量导入题目：解析 Excel 并逐行创建题目
     * Excel 格式：
     *   列A: questionName     题目标题
     *   列B: questionScore    分值
     *   列C: questionTypeId   题型(1=单选 2=多选 3=判断)
     *   列D: questionLevelId  难度(1=简单 2=中等 3=困难)
     *   列E: questionCategoryId 分类ID
     *   列F: questionDescription 题目描述
     *   列G~J: option1~option4 四个选项内容
     *   列K: answer 正确答案(选项序号，多选用逗号分隔，如 "1,3")
     */
    @Override
    public int batchImportQuestions(byte[] data, String creatorId) {
        int successCount = 0;
        // P1 修复 [B-06]：预加载所有有效的 typeId/levelId/categoryId，避免导入脏数据
        Set<Integer> validTypeIds = questionTypeRepository.findAll().stream()
                .map(QuestionType::getQuestionTypeId).collect(Collectors.toSet());
        Set<Integer> validLevelIds = questionLevelRepository.findAll().stream()
                .map(QuestionLevel::getQuestionLevelId).collect(Collectors.toSet());
        Set<Integer> validCategoryIds = questionCategoryRepository.findAll().stream()
                .map(QuestionCategory::getQuestionCategoryId).collect(Collectors.toSet());
        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory
                .create(new java.io.ByteArrayInputStream(data))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            // 从第1行开始（跳过表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                try {
                    String questionName = getCellString(row.getCell(0));
                    if (questionName.isEmpty()) {
                        continue;
                    }
                    int score = getCellInt(row.getCell(1), 2);
                    int typeId = getCellInt(row.getCell(2), 1);
                    int levelId = getCellInt(row.getCell(3), 1);
                    int categoryId = getCellInt(row.getCell(4), 1);
                    String description = getCellString(row.getCell(5));

                    // P1 修复 [B-06]：校验外键存在性，跳过无效数据行
                    if (!validTypeIds.contains(typeId)) {
                        log.warn("批量导入第{}行失败: 无效的题型ID {}", i + 1, typeId);
                        continue;
                    }
                    if (!validLevelIds.contains(levelId)) {
                        log.warn("批量导入第{}行失败: 无效的难度ID {}", i + 1, levelId);
                        continue;
                    }
                    if (!validCategoryIds.contains(categoryId)) {
                        log.warn("批量导入第{}行失败: 无效的分类ID {}", i + 1, categoryId);
                        continue;
                    }

                    // 创建选项
                    List<QuestionOption> optionList = new ArrayList<>();
                    List<String> answerIndices = new ArrayList<>();
                    String answerStr = getCellString(row.getCell(10));
                    for (String s : answerStr.split(",")) {
                        s = s.trim();
                        if (!s.isEmpty()) {
                            answerIndices.add(s);
                        }
                    }

                    for (int j = 0; j < 4; j++) {
                        String optionContent = getCellString(row.getCell(6 + j));
                        if (optionContent.isEmpty()) {
                            break;
                        }
                        QuestionOption option = new QuestionOption();
                        option.setQuestionOptionId(IdUtil.simpleUUID());
                        option.setQuestionOptionContent(optionContent);
                        optionList.add(option);
                    }
                    questionOptionRepository.saveAll(optionList);

                    // 组装选项ID和答案ID字符串
                    String optionIds = "";
                    String answerOptionIds = "";
                    for (int j = 0; j < optionList.size(); j++) {
                        optionIds += optionList.get(j).getQuestionOptionId() + "-";
                        // 答案序号是1-based
                        if (answerIndices.contains(String.valueOf(j + 1))) {
                            answerOptionIds += optionList.get(j).getQuestionOptionId() + "-";
                        }
                    }
                    optionIds = replaceLastSeparator(optionIds);
                    answerOptionIds = replaceLastSeparator(answerOptionIds);

                    // 创建题目
                    Question question = new Question();
                    question.setQuestionId(IdUtil.simpleUUID());
                    question.setQuestionName(questionName);
                    question.setQuestionScore(score);
                    question.setQuestionCreatorId(creatorId);
                    question.setQuestionLevelId(levelId);
                    question.setQuestionTypeId(typeId);
                    question.setQuestionCategoryId(categoryId);
                    question.setQuestionDescription(description);
                    question.setQuestionOptionIds(optionIds);
                    question.setQuestionAnswerOptionIds(answerOptionIds);
                    question.setQuestionVisible(1);
                    question.setCreateTime(new Date());
                    question.setUpdateTime(new Date());
                    questionRepository.save(question);
                    successCount++;
                } catch (Exception e) {
                    log.warn("批量导入第{}行失败: {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new ExamException(ResultEnum.PARAM_ERR.getCode(), "Excel 解析失败: " + e.getMessage());
        }
        log.info("批量导入题目完成，成功{}条", successCount);
        // P-08 修复：审计日志 - 批量导入题目
        AuditLogger.success(creatorId, "QUESTION_BATCH_IMPORT", null, "count=" + successCount);
        return successCount;
    }

    /** 工具：安全读取单元格字符串 */
    private String getCellString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /** 工具：安全读取单元格整数 */
    private int getCellInt(org.apache.poi.ss.usermodel.Cell cell, int defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            default:
                return defaultValue;
        }
    }

    /**
     * S-01 修复：设置考试手动状态（教师手动结束/恢复考试）
     */
    @Override
    public void setExamManualStatus(String examId, Integer manualStatus, String userId, Integer roleId) {
        // 校验所有权
        Exam exam = checkExamOwnership(examId, userId, roleId);
        // 校验 manualStatus 合法性
        if (manualStatus == null || (manualStatus != 0 && manualStatus != 1 && manualStatus != 2)) {
            throw new ExamException(ResultEnum.PARAM_ERR.getCode(), "非法的手动状态值，仅支持 0=恢复, 1=手动结束, 2=手动暂停");
        }
        exam.setExamManualStatus(manualStatus == 0 ? null : manualStatus);
        exam.setUpdateTime(new Date());
        examRepository.save(exam);
        log.info("教师设置考试手动状态：考试{}状态改为{}", examId, manualStatus);
    }

    /**
     * I-12 修复：学生删除自己的考试记录
     * 业务规则：学生只能删除自己的记录；考试必须在 ENDED 状态（防止删除进行中的答题记录）
     * ADMIN 可删除任何记录
     */
    @Override
    public void deleteExamRecord(String recordId, String userId, Integer roleId) {
        ExamRecord record = examRecordRepository.findById(recordId)
                .orElseThrow(() -> new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试记录不存在"));
        // 所有权校验：非 ADMIN 只能删除自己的记录
        boolean isAdmin = roleId != null && roleId.equals(RoleEnum.ADMIN.getId());
        if (!isAdmin && !userId.equals(record.getExamJoinerId())) {
            throw new ExamException(-3, "无权删除他人考试记录");
        }
        // 防御性校验：考试必须已结束，避免删除进行中的草稿记录
        ExamStatusEnum status = computeExamStatus(record.getExamId());
        if (status == ExamStatusEnum.AVAILABLE) {
            throw new ExamException(ResultEnum.ORDER_STATUS_ERR.getCode(), "考试进行中，无法删除记录");
        }
        examRecordRepository.delete(record);
        log.info("用户{}删除考试记录：recordId={}, examId={}", userId, recordId, record.getExamId());
        // P-08 修复：审计日志 - 删除考试记录
        AuditLogger.success(userId, "EXAM_RECORD_DELETE", recordId,
                "examId=" + record.getExamId() + ",joinerId=" + record.getExamJoinerId());
    }

    /**
     * I-12 修复：教师/管理员重置考试记录（删除记录，允许学生重考）
     * 业务规则：仅考试创建者或 ADMIN 可调用；删除后学生可重新参加考试
     */
    @Override
    public void resetExamRecord(String recordId, String userId, Integer roleId) {
        ExamRecord record = examRecordRepository.findById(recordId)
                .orElseThrow(() -> new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试记录不存在"));
        // 所有权校验：必须是考试创建者或 ADMIN
        checkExamOwnership(record.getExamId(), userId, roleId);
        examRecordRepository.delete(record);
        log.info("教师/管理员{}重置考试记录：recordId={}, examId={}, joinerId={}",
                userId, recordId, record.getExamId(), record.getExamJoinerId());
        // P-08 修复：审计日志 - 重置考试记录
        AuditLogger.success(userId, "EXAM_RECORD_RESET", recordId,
                "examId=" + record.getExamId() + ",joinerId=" + record.getExamJoinerId());
    }

    /**
     * 辅助方法：基于当前时间和手动状态计算考试状态（不修改 DB）
     * 用于 deleteExamRecord 中判断考试是否仍可用
     */
    private ExamStatusEnum computeExamStatus(String examId) {
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) {
            return ExamStatusEnum.ENDED;
        }
        // 优先检查手动状态
        Integer manualStatus = exam.getExamManualStatus();
        if (manualStatus != null) {
            if (manualStatus == 1) return ExamStatusEnum.ENDED;
            if (manualStatus == 2) return ExamStatusEnum.UPCOMING;
        }
        // 基于时间自动判断
        Date now = new Date();
        Date start = exam.getExamStartDate();
        Date end = exam.getExamEndDate();
        if (start != null && now.before(start)) return ExamStatusEnum.UPCOMING;
        if (end != null && now.after(end)) return ExamStatusEnum.ENDED;
        return ExamStatusEnum.AVAILABLE;
    }
}
