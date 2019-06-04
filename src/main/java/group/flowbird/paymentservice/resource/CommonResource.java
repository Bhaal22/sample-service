package group.flowbird.paymentservice.resource;

import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/")
@Api
public class CommonResource {
    @GetMapping("index*")
    public ModelAndView redirectToSwagger(ModelMap model){
        return new ModelAndView("redirect:/swagger-ui.html", model);
    }


}
