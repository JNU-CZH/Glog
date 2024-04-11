package com.douyuehan.doubao.config;
import com.douyuehan.doubao.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * 定时任务配置类
 *
 * @Author: ChenZhiHui
 * @DateTime: 2024/2/29 19:55
 **/
// 配置 -> 数据库 -> 调用
@Configuration
public class QuartzConfig {

    // FactoryBean可简化Bean的实例化过程:
    // 1.通过FactoryBean封装Bean的实例化过程.
    // 2.将FactoryBean装配到Spring容器里.
    // 3.将FactoryBean注入给其他的Bean.
    // 4.该Bean得到的是FactoryBean所管理的对象实例.

    // 刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        // 创建JobDetailFactoryBean，用于创建JobDetail实例
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        // 设置任务类为PostScoreRefreshJob
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        // 设置任务名称
        factoryBean.setName("postScoreRefreshJob");
        // 设置任务组名
        factoryBean.setGroup("communityJobGroup");
        // 设置任务持久性，即任务在执行完后仍然保留在JobStore中
        factoryBean.setDurability(true);
        // 设置任务失败后是否重新执行
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        // 创建SimpleTriggerFactoryBean，用于创建Trigger实例
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        // 设置触发器关联的JobDetail
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        // 设置触发器名称
        factoryBean.setName("postScoreRefreshTrigger");
        // 设置触发器组名
        factoryBean.setGroup("communityTriggerGroup");
        // 设置触发器的重复间隔时间，单位为毫秒
        factoryBean.setRepeatInterval(1000 * 300 * 1);
        // 设置触发器的JobDataMap，用于传递给任务执行的数据
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

}

