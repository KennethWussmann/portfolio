package de.ketrwu.portfolio.controller.control;

import de.ketrwu.portfolio.controller.content.TextPageController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectionController {

    @Autowired
    private TextPageController textPageController;

    @GetMapping("/impressum")
    private String impressum() {
        return textPageController.getTextPage("imprint");
    }

    @GetMapping("/datenschutz")
    private String datenschutz() {
        return textPageController.getTextPage("privacy");
    }

}
