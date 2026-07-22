/***********************************************************
 * @Description : Swagger2的配置
 * @author      : 梁山广(Laing Shan Guang)
 * @date        : 2019-05-15 07:39
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package lsgwr.exam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableSwagger2
public class Swagger2Config {
    @Bean
    public Docket api() {

        ParameterBuilder ticketPar = new ParameterBuilder();
        List<Parameter> pars = new ArrayList<>();
        ticketPar.name("Access-Token").description("Rest接口权限认证token,无需鉴权可为空")
                .modelRef(new ModelRef("string")).parameterType("header")
                //header中的ticket参数非必填，传空也可以
                .required(false).build();
        //根据每个方法名也知道当前方法在设置什么参数
        pars.add(ticketPar.build());

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 自行修改为自己的包路径
                .apis(RequestHandlerSelectors.basePackage("lsgwr"))
                .paths(PathSelectors.any())
                .build()
                .globalOperationParameters(pars);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("在线考试系统 API")
                .description("Spring Boot 在线考试系统接口文档\n\n" +
                        "## 模块分组\n" +
                        "- 用户认证 / 注册 / 个人信息（UserController）\n" +
                        "- 题库管理（QuestionController，含批量导入）\n" +
                        "- 考试管理（ExamController，含随机组卷、班级排名、主观题批改）\n" +
                        "- 文件上传下载（FileController）\n\n" +
                        "## 鉴权说明\n" +
                        "所有需鉴权接口需在请求头携带 `Access-Token`（JWT）。\n" +
                        "部分接口需要 `@RoleRequired({TEACHER, ADMIN})` 角色权限。")
                .termsOfServiceUrl("https://github.com/fssl168/spring-boot-online-exam")
                .version("2.0")
                .contact(new Contact("liangshanguang", "https://github.com/fssl168/spring-boot-online-exam", "liangshanguang2@gmail.com"))
                .build();
    }
}