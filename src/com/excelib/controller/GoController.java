/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.excelib.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class GoController implements EnvironmentAware{

    private final Log logger = LogFactory.getLog(GoController.class);

    private Environment environment = null;
    //处理HEAD类型的”/”请求
    @RequestMapping(value = {"/"}, method = {RequestMethod.HEAD})
    public String head() {
        return "go.jsp";
    }

    //处理GET类型的"/index"和”/”请求
    @RequestMapping(value = {"/index", "/"}, method = {RequestMethod.GET})
    public String index(Model model) throws Exception {
        logger.info("======processed by index=======");
        //返回msg参数
        model.addAttribute("msg", "Go Go Go!");
        return "go.jsp";
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
