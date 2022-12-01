package fr.ans.psc.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class PatientRecordingController {

    @GetMapping("/secure/patient")
    public String navigate() {
        return "new-patient";
    }
}
