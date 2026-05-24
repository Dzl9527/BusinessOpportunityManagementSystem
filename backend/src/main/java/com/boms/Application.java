package com.boms;

import com.boms.model.Opportunity;
import com.boms.model.TaskItem;
import com.boms.model.ActivityLog;
import com.boms.repository.OpportunityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner demoDataSeeder(OpportunityRepository oppRepository,
                                            @Value("${app.seed-demo-data:false}") boolean seedDemoData) {
        return args -> {
            if (!seedDemoData) {
                return;
            }
            if (oppRepository.count() > 0) {
                return; // Database already seeded
            }

            // --- Seed Opportunity 1 ---
            Opportunity opp1 = new Opportunity();
            opp1.setName("智能客服系统采购项目");
            opp1.setCompany("极客科技股份有限公司");
            opp1.setStage("proposal");
            opp1.setValue(280000.0);
            opp1.setProbability(60);
            opp1.setCloseDate("2026-06-15");
            opp1.setContactName("陈总");
            opp1.setContactPhone("13800138000");
            opp1.setContactEmail("chen.y@geektech.com");
            opp1.setSource("线上注册");
            opp1.setPriority("high");
            opp1.setOwner("张经理");
            opp1.setDescription("客户正在寻找一套能够对接微信公众号、小程序和官网的智能客服系统，需要支持自然语言处理（NLP）和多轮对话。目前已完成需求对接，正在起草定制化方案和报价。");

            TaskItem t1_1 = new TaskItem("定制化方案方案书编写", true, opp1);
            TaskItem t1_2 = new TaskItem("系统演示与技术答辩", true, opp1);
            TaskItem t1_3 = new TaskItem("提交正式报价单", false, opp1);
            TaskItem t1_4 = new TaskItem("进行第一轮商务合同条款确认", false, opp1);
            opp1.setTasks(new ArrayList<>(Arrays.asList(t1_1, t1_2, t1_3, t1_4)));

            ActivityLog a1_1 = new ActivityLog("system", "创建商机：智能客服系统采购项目，分配给 张经理", "2026-05-10 10:00", opp1);
            ActivityLog a1_2 = new ActivityLog("meeting", "与陈总及技术总监进行线上会议，调研核心客服指标和对接系统", "2026-05-12 14:30", opp1);
            ActivityLog a1_3 = new ActivityLog("call", "致电陈总确认接口细节，客户表示对我们的知识库管理功能非常感兴趣", "2026-05-15 11:15", opp1);
            ActivityLog a1_4 = new ActivityLog("system", "商机阶段由 [资质评估] 变更为 [方案报价]", "2026-05-18 16:45", opp1);
            opp1.setActivities(new ArrayList<>(Arrays.asList(a1_1, a1_2, a1_3, a1_4)));

            // --- Seed Opportunity 2 ---
            Opportunity opp2 = new Opportunity();
            opp2.setName("云原生容器化改造服务");
            opp2.setCompany("瑞丰物流集团");
            opp2.setStage("negotiation");
            opp2.setValue(650000.0);
            opp2.setProbability(80);
            opp2.setCloseDate("2026-06-05");
            opp2.setContactName("李处长");
            opp2.setContactPhone("13911022938");
            opp2.setContactEmail("li.xiang@ruifeng.cn");
            opp2.setSource("转介绍");
            opp2.setPriority("high");
            opp2.setOwner("李主管");
            opp2.setDescription("物流调度系统进行微服务及容器化改造，需提供Kubernetes集群规划、CI/CD流水线建设及为期半年的运维保障。目前报价已被接受，正处于合同条款博弈阶段，重点在赔偿上限及响应时间SLA上。");

            TaskItem t2_1 = new TaskItem("架构设计初稿设计", true, opp2);
            TaskItem t2_2 = new TaskItem("商务报价审批", true, opp2);
            TaskItem t2_3 = new TaskItem("法务审核合同模板", true, opp2);
            TaskItem t2_4 = new TaskItem("确定SLA保障细则", false, opp2);
            opp2.setTasks(new ArrayList<>(Arrays.asList(t2_1, t2_2, t2_3, t2_4)));

            ActivityLog a2_1 = new ActivityLog("system", "创建商机，由渠道商推荐，分配给 李主管", "2026-04-20 09:12", opp2);
            ActivityLog a2_2 = new ActivityLog("meeting", "前往瑞丰物流总部与运维负责人交流，评估迁移风险", "2026-04-25 15:00", opp2);
            ActivityLog a2_3 = new ActivityLog("email", "发送更新版云架构方案及POC测试报告", "2026-05-02 18:22", opp2);
            ActivityLog a2_4 = new ActivityLog("system", "商机阶段变更为 [谈判协商]，预计赢单率提升至 80%", "2026-05-20 14:00", opp2);
            opp2.setActivities(new ArrayList<>(Arrays.asList(a2_1, a2_2, a2_3, a2_4)));

            // --- Seed Opportunity 3 ---
            Opportunity opp3 = new Opportunity();
            opp3.setName("企业级数据湖平台建设");
            opp3.setCompany("华夏零售连锁");
            opp3.setStage("qualification");
            opp3.setValue(1200000.0);
            opp3.setProbability(40);
            opp3.setCloseDate("2026-08-30");
            opp3.setContactName("王总");
            opp3.setContactPhone("13588992233");
            opp3.setContactEmail("wang.jian@huaxaretail.com");
            opp3.setSource("展会活动");
            opp3.setPriority("medium");
            opp3.setOwner("张经理");
            opp3.setDescription("客户希望打通线下POS、线上电商及小程序的用户行为数据，构建统一的数据湖。由于项目预算庞大，目前正处于评估我司方案与竞争对手阶段，需要协调资深数据架构师做联合售前输出。");

            TaskItem t3_1 = new TaskItem("了解客户历史数据架构", true, opp3);
            TaskItem t3_2 = new TaskItem("准备行业零售数据湖案例PPT", false, opp3);
            TaskItem t3_3 = new TaskItem("协调内部架构师资源", false, opp3);
            opp3.setTasks(new ArrayList<>(Arrays.asList(t3_1, t3_2, t3_3)));

            ActivityLog a3_1 = new ActivityLog("system", "创建商机：在行业零售峰会中获得联系方式，分配给 张经理", "2026-05-18 11:30", opp3);
            ActivityLog a3_2 = new ActivityLog("call", "与王总电话沟通，初步摸清了对方在多渠道数据融合方面的痛点", "2026-05-22 10:15", opp3);
            opp3.setActivities(new ArrayList<>(Arrays.asList(a3_1, a3_2)));

            // --- Seed Opportunity 4 ---
            Opportunity opp4 = new Opportunity();
            opp4.setName("私有云存储扩容项目");
            opp4.setCompany("天图设计院");
            opp4.setStage("prospecting");
            opp4.setValue(150000.0);
            opp4.setProbability(20);
            opp4.setCloseDate("2026-07-20");
            opp4.setContactName("赵工");
            opp4.setContactPhone("15677889900");
            opp4.setContactEmail("zhao.y@tiantu-design.com");
            opp4.setSource("冷拓拜访");
            opp4.setPriority("low");
            opp4.setOwner("王销售");
            opp4.setDescription("设计院日常三维渲染图纸增多，NAS存储告急，打算扩容私有存储。目前处于初步接触阶段，需要带硬件产品经理上门做硬件测试及空间评估。");

            TaskItem t4_1 = new TaskItem("致电客户约见上门时间", false, opp4);
            TaskItem t4_2 = new TaskItem("准备存储服务器硬件彩页", false, opp4);
            opp4.setTasks(new ArrayList<>(Arrays.asList(t4_1, t4_2)));

            ActivityLog a4_1 = new ActivityLog("system", "王销售录入商机，归属于发现阶段", "2026-05-23 15:40", opp4);
            opp4.setActivities(new ArrayList<>(Arrays.asList(a4_1)));

            // --- Save All ---
            oppRepository.saveAll(Arrays.asList(opp1, opp2, opp3, opp4));
        };
    }
}
