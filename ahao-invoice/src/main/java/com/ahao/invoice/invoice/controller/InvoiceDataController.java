package com.ahao.invoice.invoice.controller;

import com.ahao.commons.entity.AjaxDTO;
import com.ahao.commons.spring.config.SpringConfig;
import com.ahao.commons.util.lang.math.NumberHelper;
import com.ahao.invoice.invoice.entity.InvoiceDO;
import com.ahao.invoice.invoice.service.InvoiceService;
import com.ahao.invoice.invoice.util.ValidUtils;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Created by Ahaochan on 2017/7/30.
 */
@RestController
public class InvoiceDataController {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceDataController.class);
    private InvoiceService invoiceService;

    @Autowired
    public InvoiceDataController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/invoice")
    @Transactional
    public AjaxDTO add(@RequestParam("goods") Long goodsId,
                       @RequestParam("goodsNumber") Long number,
                       @Valid InvoiceDO validInvoice, BindingResult result) {
        // 后端验证是否出错
        if (result.hasErrors()) {
            AjaxDTO ajax = AjaxDTO.failure(SpringConfig.getString("insert.failure", validInvoice.getId()));
            // 返回以field为键, List<错误信息> 为值的Map集合
            ajax.setObj(ValidUtils.paraseErrors(result));
            return ajax;
        }

        invoiceService.insert(validInvoice);
        invoiceService.addRelate(validInvoice.getId(), goodsId, number);

        Long id = validInvoice.getId();
        return AjaxDTO.success(SpringConfig.getString("insert.success", id), id);
    }

    @DeleteMapping("/invoices")
    @Transactional
    public AjaxDTO delete(@RequestBody MultiValueMap<String, String> formData) {
        List<String> ids = formData.get("invoiceIds[]");

        boolean success = invoiceService.deleteByKey(ids);
        int flag = NumberHelper.parseInt(success);
        String msg = SpringConfig.getString(success ? "delete.success" : "delete.failure");
        return AjaxDTO.get(flag, msg);
    }

    @GetMapping("/invoice/page")
    public JSONObject getByPage(Integer page) {
        JSONObject json = new JSONObject();
        json.put("total", invoiceService.getAllCount());
        json.put("rows", invoiceService.getByPage(page));
        return json;
    }

    @PostMapping("/invoice/{invoiceId}")
    @Transactional
    public AjaxDTO modify(@RequestParam("goods") Long goodsId,
                          @RequestParam("goodsNumber") Long number,
                          @Valid InvoiceDO validInvoice, BindingResult result) {
        // 后端验证是否出错
        if (result.hasErrors()) {
            AjaxDTO ajax = AjaxDTO.failure(SpringConfig.getString("insert.failure", validInvoice.getId()));
            // 返回以field为键, List<错误信息> 为值的Map集合
            ajax.setObj(ValidUtils.paraseErrors(result));
            return ajax;
        }
        if (validInvoice.getId() == null) {
            // ID不能为空, 否则会更新全部数据
            return AjaxDTO.failure(SpringConfig.getString("update.failure", validInvoice.getId()));
        }

        invoiceService.update(validInvoice);
        invoiceService.addRelate(validInvoice.getId(), goodsId, number);
        return AjaxDTO.success(SpringConfig.getString("insert.success", validInvoice.getId()));
    }


    @PostMapping("/invoice/check")
    public boolean checkUnique(String code, String number) {
        boolean isExist = invoiceService.exist(code, number);
        return !isExist;
    }
}
