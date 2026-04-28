package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.example.demo.security.MyUserDetails;

@Controller
public class TrashController {

    @Autowired
    private TrashService trashService;

    // 削除一覧
    @GetMapping("/trash")
    public String trashList(
            @AuthenticationPrincipal MyUserDetails userDetails,
            Model model
    ) 
    
    {
        if (userDetails == null) return "redirect:/login";

        String username = userDetails.getUser().getUsername();
        model.addAttribute("trashList", trashService.findByUsername(username));

        return "trash-list";
    }

    // 復元
    @PostMapping("/trash/{id}/restore")
    public String restore(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails
    ) 
            
    {
        if (userDetails == null) return "redirect:/login";

        trashService.restore(id, userDetails.getUser().getUsername());
        return "redirect:/trash";
    }

    // 完全削除
    @PostMapping("/trash/{id}/destroy")
    public String destroy(
            @PathVariable Long id,
            @AuthenticationPrincipal MyUserDetails userDetails
    ) 
            
    {
        if (userDetails == null) return "redirect:/login";

        trashService.deleteForever(id, userDetails.getUser().getUsername());
        return "redirect:/trash";
    }
}
