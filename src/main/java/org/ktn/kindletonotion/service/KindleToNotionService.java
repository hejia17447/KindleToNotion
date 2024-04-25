package org.ktn.kindletonotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ktn.kindletonotion.kindle.model.Book;
import org.ktn.kindletonotion.kindle.model.Mark;
import org.ktn.kindletonotion.model.notion.block.type.*;
import org.ktn.kindletonotion.notion.NotionClient;
import org.ktn.kindletonotion.notion.model.Block;
import org.ktn.kindletonotion.notion.model.page.PageData;
import org.ktn.kindletonotion.notion.model.page.PageProperties;
import org.ktn.kindletonotion.notion.utils.JsonUtil;
import org.ktn.kindletonotion.utils.PageListToMapUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * kindle上传notion服务
 * @author 贺佳
 */
@Service
public class KindleToNotionService {

    private final NotionClient notionClient;

    public KindleToNotionService(NotionClient notionClient) {
        this.notionClient = notionClient;
    }

    /**
     * 把数据库页下面的页转化为Map
     * @param pageDataList 页面数据
     * @return 页面数据Map
     */
    public Map<String, PageData> pagesToMap(List<PageData> pageDataList) {
        // 把数据库页下面的页转化为Map
        return PageListToMapUtil.toPgeMap(pageDataList);
    }

    public void uploadBookNote(String bookName, Book book, Map<String, PageData> pageMap) {

        if (pageMap.containsKey(bookName)) {
            // notion中存在
            // 获取页面数据
            PageData pageData = pageMap.get(bookName);
            // 获取页的id
            String pageId = pageData.getId();
            // 获取notion中笔记个数
            int markNum = pageData.getProperties().get("笔记数").get("number").asInt();
            // 如果kindle中的笔记数量与notion中的笔记数量一致则不进行更新
            if (markNum == book.getNums()) {
                return;
            }
            // 计算notion的页大小，笔记数*4
            int notionPageSize = markNum * 4;
            // 获取页的所有子项
            List<Block> blocks = notionClient.block.queryBlocks(pageId, notionPageSize);
            upload(book, blocks, notionPageSize, pageData);

        } else {
            // notion中不存在
            // todo 创建页面
        }

    }

    private void upload(Book book, List<Block> blocks, int notionPageSize, PageData pageData) {
        // 最后标记时间
        LocalDateTime lastMarkTime = LocalDateTime.of(1900, 1, 1, 0, 0, 0);

        // 标注总数
        Integer totalNum = book.getNums();
        // 获取所有标注
        List<Mark> marks = book.getMarks();
        // i:当前已遍历的标注数,j:当前notion子项的下标
        int i = 1, j = 0;
        // 遍历所有标注进行上传
        for (; i < totalNum + 1; i++) {
            // 获取标注
            Mark mark = marks.get(i - 1);
            lastMarkTime = lastMarkTime.isAfter(mark.getTime()) ?  mark.getTime() : lastMarkTime;

            // 当前的标注未超出notion页已有的笔记数，即（i-1）*4<=notionPageSize
            if ((i - 1) * 4 <= notionPageSize) {
                // 更新笔记

                // 获取积木id
                String blockId = blocks.get(j++).getId();
                // 更新callout请求体
                Callout callout = new Callout(mark.getContent());
                String requestBody = JsonUtil.toJson(callout);
                // 发送更新请求
                notionClient.block.updateBlock(blockId, requestBody);

                // 获取积木id
                blockId = blocks.get(j++).getId();
                // 更新quote请求体
                Quote quote = new Quote(mark.getHaveNote() ? mark.getNote() : "无");
                requestBody = JsonUtil.toJson(quote);
                // 发送更新请求
                notionClient.block.updateBlock(blockId, requestBody);

                // 获取积木id
                blockId = blocks.get(j++).getId();
                // 更新paragraph请求体
                Paragraph paragraph = new Paragraph(mark.getAddress());
                requestBody = JsonUtil.toJson(paragraph);
                // 发送更新请求
                notionClient.block.updateBlock(blockId, requestBody);

                // 获取积木id
                blockId = blocks.get(j++).getId();
                // 更新divider请求体
                Divider divider = new Divider();
                requestBody = JsonUtil.toJson(divider);
                // 发送更新请求
                notionClient.block.updateBlock(blockId, requestBody);
                
            } else if ((i - 1) * 4 > notionPageSize) {
                // 如果笔记数大于当前notion笔记则进行追加笔记
                Children children = getChildren(mark);
                // 创建请求体
                String requestBody = JsonUtil.toJson(children);
                // 发送追加请求
                notionClient.block.additionBlock(pageData.getId(), requestBody);

            }
        }
        // 如果当前笔记少于notion中的笔记则删除多余的子项
        if (i * 4 < notionPageSize) {
            for (; j < notionPageSize; j++) {
                String blockId = blocks.get(j).getId();
                notionClient.block.deleteBlock(blockId);
            }
        }
        // 更新页面属性
        PageProperties properties = new PageProperties(book.getNums(), lastMarkTime.toString());
        String requestBody = JsonUtil.toJson(properties);
        // 发送更新请求
        notionClient.page.updatePageProperties(pageData.getId(), requestBody);

    }

    private static Children getChildren(Mark mark) {
        ObjectMapper mapper = new ObjectMapper();
        Children children = new Children();

        // 创建callout
        Callout callout = new Callout(mark.getContent(), "gray_background");
        children.setCallout(mapper.valueToTree(callout));
        // 创建quote
        Quote quote = new Quote(mark.getHaveNote() ? mark.getNote() : "无");
        children.setQuote(mapper.valueToTree(quote));
        // 创建paragraph
        Paragraph paragraph = new Paragraph(mark.getAddress(), "");
        children.setParagraph(mapper.valueToTree(paragraph));
        // 创建divider
        Divider divider = new Divider();
        children.setDivider(mapper.valueToTree(divider));
        return children;
    }


}
