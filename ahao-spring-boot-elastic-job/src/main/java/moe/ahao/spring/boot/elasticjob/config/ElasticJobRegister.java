package moe.ahao.spring.boot.elasticjob.config;

import com.ahao.util.commons.lang.reflect.ClassHelper;
import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import moe.ahao.spring.boot.elasticjob.job.capable.ElasticJobListenerCapable;
import moe.ahao.spring.boot.elasticjob.job.capable.JobEventTraceDataSourceCapable;
import moe.ahao.spring.boot.elasticjob.properties.base.BaseJobProperties;
import moe.ahao.spring.boot.elasticjob.properties.base.DefaultJobProperties;
import moe.ahao.spring.boot.elasticjob.properties.base.ElasticAllJobProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.sql.DataSource;
import java.util.Objects;

public class ElasticJobRegister implements InitializingBean, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(ElasticJobRegister.class);

    private ZookeeperRegistryCenter zk;
    private ApplicationContext ctx;
    private ElasticAllJobProperties jobConfig;

    public ElasticJobRegister(ZookeeperRegistryCenter zk, ElasticAllJobProperties jobConfig) {
        this.zk = zk;
        this.jobConfig = jobConfig;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        DefaultJobProperties defaultProperties = jobConfig.getBase();

        jobConfig.getSimple().forEach(p -> init(defaultProperties, p));
        jobConfig.getDataFlow().forEach(p -> init(defaultProperties, p));
        jobConfig.getScript().forEach(p -> init(defaultProperties, p));
    }

    private void init(DefaultJobProperties defaultProperties, BaseJobProperties jobProperties) {
        String beanName = jobProperties.getBeanName();
        ElasticJob elasticJob = StringUtils.isEmpty(beanName) ? null : ctx.getBean(jobProperties.getBeanName(), ElasticJob.class);
        Class<?> clazz = elasticJob == null ? null : elasticJob.getClass();

        JobCoreConfiguration coreConfig = jobProperties.generateJobCoreConfig(defaultProperties);
        JobTypeConfiguration typeConfig = jobProperties.generateJobTypeConfig(coreConfig, clazz);
        LiteJobConfiguration liteConfig = jobProperties.generateLiteJobConfig(defaultProperties, typeConfig);

        registerSpringJobSchedulerV2(defaultProperties, jobProperties, liteConfig, elasticJob);
    }

    private void registerSpringJobSchedulerV1(DefaultJobProperties defaultProperties, BaseJobProperties jobProperties,
                     LiteJobConfiguration liteConfig, ElasticJob job) {
        // 1. 创建 SpringJobScheduler
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
        factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);

        // 2. 传入构造器所需参数
        factory.addConstructorArgValue(job);
        factory.addConstructorArgValue(zk);
        factory.addConstructorArgValue(liteConfig);
        JobEventRdbConfiguration jobEventRdbConfiguration = null;
        ElasticJobListener[] listeners = new ElasticJobListener[0];
        if (defaultProperties.isJobEventTraceEnabled() || Objects.equals(jobProperties.getJobEventTraceEnabled(), true)) {
            if(!ClassHelper.isSubClass(job, JobEventTraceDataSourceCapable.class)) {
                throw new ClassCastException(job.getClass().getName() + "未实现 JobEventTraceDataSourceCapable 接口");
            }
            JobEventTraceDataSourceCapable capable = (JobEventTraceDataSourceCapable) job;
            DataSource dataSource = capable.getJobEventTraceDataSource();
            if(dataSource == null) {
                throw new IllegalArgumentException("事件追踪数据源为空! http://elasticjob.io/docs/elastic-job-lite/02-guide/event-trace/");
            }
            jobEventRdbConfiguration = new JobEventRdbConfiguration(dataSource);
            factory.addConstructorArgValue(jobEventRdbConfiguration);
        }
        if(ClassHelper.isSubClass(job, ElasticJobListenerCapable.class)) {
            ElasticJobListenerCapable capable = (ElasticJobListenerCapable) job;
            listeners = capable.getElasticJobListeners().toArray(new ElasticJobListener[0]);
        }
        factory.addConstructorArgValue(listeners);

        // 3. 注册到 Spring Application Context
        String jobName = jobProperties.getJobName();
        String beanName = jobName + "SpringJobScheduler";
        AbstractBeanDefinition beanDefinition = factory.getBeanDefinition();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) ctx.getAutowireCapableBeanFactory();
        if(StringUtils.isBlank(beanName)) {
            BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
        } else {
            registry.registerBeanDefinition(beanName, beanDefinition);
        }

        // 4. 启动定时任务
        SpringJobScheduler springJobScheduler = ctx.getBean(beanName, SpringJobScheduler.class);
        springJobScheduler.init();
    }

    private void registerSpringJobSchedulerV2(DefaultJobProperties defaultProperties, BaseJobProperties jobProperties,
                                              LiteJobConfiguration liteConfig, ElasticJob job) {
        // 1. 传入构造器所需参数
        JobEventRdbConfiguration jobEventRdbConfiguration = null;
        ElasticJobListener[] listeners = new ElasticJobListener[0];
        if (defaultProperties.isJobEventTraceEnabled() || Objects.equals(jobProperties.getJobEventTraceEnabled(), true)) {
            if(!ClassHelper.isSubClass(job, JobEventTraceDataSourceCapable.class)) {
                throw new ClassCastException(job.getClass().getName() + "未实现 JobEventTraceDataSourceCapable 接口");
            }
            JobEventTraceDataSourceCapable capable = (JobEventTraceDataSourceCapable) job;
            DataSource dataSource = capable.getJobEventTraceDataSource();
            if(dataSource == null) {
                throw new IllegalArgumentException("事件追踪数据源为空! http://elasticjob.io/docs/elastic-job-lite/02-guide/event-trace/");
            }
            jobEventRdbConfiguration = new JobEventRdbConfiguration(dataSource);
        }
        if(ClassHelper.isSubClass(job, ElasticJobListenerCapable.class)) {
            ElasticJobListenerCapable capable = (ElasticJobListenerCapable) job;
            listeners = capable.getElasticJobListeners().toArray(new ElasticJobListener[0]);
        }

        // 2. 创建 SpringJobScheduler
        SpringJobScheduler scheduler;
        if(jobEventRdbConfiguration == null) {
            scheduler = new SpringJobScheduler(job, zk, liteConfig, listeners);
        } else {
            scheduler = new SpringJobScheduler(job, zk, liteConfig, jobEventRdbConfiguration, listeners);
        }

        // 3. 注册到 Spring Application Context
        String jobName = jobProperties.getJobName();
        String beanName = jobName + "SpringJobScheduler";
        ConfigurableListableBeanFactory registry = (ConfigurableListableBeanFactory) ctx.getAutowireCapableBeanFactory();
        registry.registerSingleton(beanName, scheduler);

        // 4. 启动定时任务
        scheduler.init();
    }
}
