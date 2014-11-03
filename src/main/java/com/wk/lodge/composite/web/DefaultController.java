
package com.wk.lodge.composite.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class DefaultController {
    @RequestMapping(value = "/")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void index() {

    }
}
