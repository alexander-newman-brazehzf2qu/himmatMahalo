package org.cboard.services;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;
import org.cboard.jdbc.JdbcDataProvider;
import org.cboard.security.service.LocalSecurityFilter;
import org.cboard.services.persist.PersistContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by yfyuan on 2017/2/10.
 */
@Service
public class PersistService {

    private static final Logger LOG = LoggerFactory.getLogger(PersistService.class);

    @Value("${phantomjs_path}")
    private String phantomjsPath;
    private String scriptPath = new File(this.getClass().getResource("/phantom.js").getFile()).getPath();

    @Value("${web_port}")
    private String webPort;
    @Value("${web_context}")
    private String webContext;

    private static final ConcurrentMap<String, PersistContext> TASK_MAP = new ConcurrentHashMap<>();

    public PersistContext persist(Long dashboardId, String userId) {
        String persistId = UUID.randomUUID().toString().replaceAll("-", "");
        Process process = null;
        try {
            String web = webPort + "/";
            if (StringUtils.isNotBlank(webContext)) {
                web += webContext + "/";
            }
            PersistContext context = new PersistContext(dashboardId);
            TASK_MAP.put(persistId, context);
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            LocalSecurityFilter.put(uuid, userId);
            String phantomUrl = new StringBuffer("http://127.0.0.1:")
                    .append(web)
                    .append("render.html")
                    .append("?sid=").append(uuid)
                    .append("#?id=").append(dashboardId)
                    .append("&pid=").append(persistId)
                    .toString();
            String cmd = String.format("%s %s %s", phantomjsPath, scriptPath, phantomUrl);
            LOG.info("Run phantomjs command: {}", cmd);
            process = Runtime.getRuntime().exec(cmd);
            synchronized (context) {
                context.wait(10 * 60 * 1000);
            }
            process.destroy();
            TASK_MAP.remove(persistId);
            return context;
        } catch (Exception e) {
            if (process != null) {
                process.destroy();
            }
            e.printStackTrace();
        }
        return null;
    }

    public String persistCallback(String persistId, JSONObject data) {
        PersistContext context = TASK_MAP.get(persistId);
        synchronized (context) {
            context.setData(data);
            context.notify();
        }
        return "1";
    }
}
