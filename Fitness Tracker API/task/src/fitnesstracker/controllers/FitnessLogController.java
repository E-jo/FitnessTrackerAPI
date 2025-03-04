package fitnesstracker.controllers;

import fitnesstracker.models.*;
import fitnesstracker.services.ApplicationService;
import fitnesstracker.services.FitnessLogService;
import fitnesstracker.services.UserService;
import fitnesstracker.services.TokenBucketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger.*;
import java.time.LocalDateTime;


import java.security.SecureRandom;
import java.util.*;

@RestController
public class FitnessLogController {
    @Autowired
    FitnessLogService fitnessLogService;

    @Autowired
    UserService userService;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    TokenBucketService tokenBucketService;

    @Autowired
    PasswordEncoder encoder;

    @GetMapping("/api/tracker")
    public ResponseEntity<?> getAllFitnessLogs(HttpServletRequest request) {
        System.out.println("Checking header...");
        boolean validHeader = false;

        Enumeration<String> headerNames = request.getHeaderNames();

        Application application = new Application();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println(headerName);
            if (headerName.equalsIgnoreCase("x-api-key")) {
                String key = request.getHeader(headerName);
                System.out.println("Found X-API-Key header with key: " + key);
                Optional<Application> optionalApplication = applicationService.findByApiKey(key);
                if (optionalApplication.isPresent()) {
                    validHeader = true;
                    application = optionalApplication.get();
                    break;
                }
            }
        }

        if (!validHeader) {
            System.out.println("Invalid/no API key found in header X-API-Key");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        System.out.println("Requesting app: " + application.getName());
        tokenBucketService.getTokenBuckets().forEach((appName, tokens) -> {
            System.out.println("App: " + appName + ": " + tokens);
        });

        try {
            System.out.println("Attempting to retrieve tokens from " + application.getName());
            String category = application.getCategory();
            System.out.println(application.getName() + " category: " + category);

            boolean valid = category.equalsIgnoreCase("premium") ? true : tokenBucketService.grantAccess(application.getName());

            int tokens = tokenBucketService.getTokenBuckets().get(application.getName()).getTokens().intValue();
            System.out.println(application.getName() + " has " + tokens + " tokens remaining");

            System.out.println("Return value of grantAccess for " + application.getName() +
                    ": " + valid);

            if (!valid) {
                System.out.println("Access denied");
                System.out.println(application.getName() + " has " + tokens + " tokens");
                return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
            }
        } catch (Exception e) {
            System.out.println("Caught: " + e.getMessage());
            // pass
        }

        return new ResponseEntity<>(fitnessLogService.findAllByOrderByCreatedAtDesc(),
                HttpStatus.OK);
    }

    @PostMapping("/api/tracker")
    public ResponseEntity<?> saveFitnessLog(@RequestBody FitnessLog fitnessLog,
                                            HttpServletRequest request) {

        boolean validHeader = false;

        Enumeration<String> headerNames = request.getHeaderNames();

        Application application = new Application();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println("Header name: " + headerName);
            if (headerName.equalsIgnoreCase("x-api-key")) {
                String key = request.getHeader(headerName);
                System.out.println("Found X-API-Key header with key: " + key);
                Optional<Application> optionalApplication = applicationService.findByApiKey(key);
                if (optionalApplication.isPresent()) {
                    validHeader = true;
                    application = optionalApplication.get();
                    break;
                }
            }
        }

        if (!validHeader) {
            System.out.println("No valid header API key found");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        fitnessLog.setApplication(application.getName());

        fitnessLogService.save(fitnessLog);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/api/developers/signup")
    public ResponseEntity<?> register(@RequestBody @Valid User user) {
        Optional<User> userOptional = userService.findUserByEmail(user.getEmail());
        if (userOptional.isPresent()) {
            System.out.println("User " + user.getEmail() + " already exists");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        System.out.println("Raw password: " + user.getPassword());
        user.setPassword(encoder.encode(user.getPassword()));
        System.out.println("Encoded password: " + user.getPassword());
        User newUser = userService.save(user);
        System.out.println(newUser);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(UriComponentsBuilder.fromPath("/api/developers/{id}")
                .buildAndExpand(newUser.getId())
                .toUri());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @GetMapping("/api/developers/{id}")
    public ResponseEntity<?> getDeveloperProfile(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
                                                 @PathVariable("id") Long id) {
        Optional<User> userOptional = userService.findUserByEmail(userDetails.getUsername());
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        User user = userOptional.get();
        if (!Objects.equals(user.getId(), id)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        userOptional = userService.findUserById(id);
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        User developer = userOptional.get();
        List<Application> timeSortedApps = developer.getApplications();
        Comparator<Application> mostRecent = Comparator.comparing(Application::getCreatedAt).reversed();
        timeSortedApps.sort(mostRecent);
        DeveloperProfile profile = new DeveloperProfile(developer.getId(),
                developer.getEmail(), timeSortedApps);
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }


    @PostMapping("/api/applications/register")
    public ResponseEntity<?> registerApplication(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
                                                 @RequestBody @Valid ApplicationSubmission applicationSubmission) {

        Optional<Application> optionalApplication = applicationService.findByName(applicationSubmission.name());
        if (optionalApplication.isPresent()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        System.out.println("Category: " + applicationSubmission.category());

        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder apiKeySb = new StringBuilder();
        boolean unique = false;

        while (!unique) {
            for (int i = 0; i < 20; i++) {
                int randomIndex = random.nextInt(characters.length());
                apiKeySb.append(characters.charAt(randomIndex));
            }
            Optional<Application> existingApiKey = applicationService.findByApiKey(apiKeySb.toString());
            if (existingApiKey.isEmpty()) {
                unique = true;
            }
        }

        String apiKey = apiKeySb.toString();

        Optional<User> userOptional = userService.findUserByEmail(userDetails.getUsername());
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>("User " + userDetails.getUsername() +
                    " not found", HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();

        Application application = new Application(applicationSubmission.name(),
                applicationSubmission.description(), apiKey, user, applicationSubmission.category());

        application.setUser(user);

        Application savedApplication = applicationService.save(application);

        String appName = application.getName();

        tokenBucketService.getTokenBuckets().putIfAbsent(appName, new TokenBucket(appName,
                new AtomicInteger(1), LocalDateTime.now(), new AtomicBoolean(true)));

        System.out.println("Current tokens for " + appName + ": " +
                tokenBucketService.getTokenBuckets().get(application.getName()));

        user.getApplications().add(savedApplication);

        return new ResponseEntity<>(new SubmissionResponse(savedApplication.getName(),
                savedApplication.getApiKey(), applicationSubmission.category()), HttpStatus.CREATED);
    }
}

