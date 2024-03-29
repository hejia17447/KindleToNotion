package org.ktn.kindletonotion.notion.service;

import lombok.extern.slf4j.Slf4j;
import org.ktn.kindletonotion.notion.config.NotionConfigProperties;
import org.ktn.kindletonotion.notion.model.Database;
import org.ktn.kindletonotion.notion.model.PageData;
import org.ktn.kindletonotion.notion.utils.HttpHeaderUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

/**
 * @author 贺佳
 * Database操作
 */
@Slf4j
@Service
public class DatabaseService {

    private final NotionConfigProperties notionConfigProps;

    private final RestTemplate restTemplate;

    private final HttpHeaderUtil httpHeaderUtil;

    public DatabaseService(NotionConfigProperties notionConfigProps, RestTemplate restTemplate, HttpHeaderUtil httpHeaderUtil) {
        this.notionConfigProps = notionConfigProps;
        this.restTemplate = restTemplate;
        this.httpHeaderUtil = httpHeaderUtil;
    }

    /**
     * 获取数据库的所有页信息
     * @param databaseId 数据库id
     * @return 页信息
     */
    public List<PageData> queryPages(String databaseId) {

        String url = notionConfigProps.apiUrl() + "/v1/databases/" + databaseId + "/query";
        log.info("查询Notion数据库：{}", url);

        ResponseEntity<Database> db = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(httpHeaderUtil.getDefaultHeaders()),
                Database.class
        );

        return Objects.requireNonNull(db.getBody()).getPageDataLIst();

    }

}
