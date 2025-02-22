import com.google.gson.Gson;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.dynamic.input.DynamicTesting;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.mocks.web.request.HttpRequest;
import org.hyperskill.hstest.mocks.web.response.HttpResponse;
import org.hyperskill.hstest.stage.SpringTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;
import static org.hyperskill.hstest.testing.expect.json.JsonChecker.any;
import static org.hyperskill.hstest.testing.expect.json.JsonChecker.isArray;
import static org.hyperskill.hstest.testing.expect.json.JsonChecker.isObject;
import static org.hyperskill.hstest.testing.expect.json.JsonChecker.isString;

public class FitnessTrackerTest extends SpringTest {
    private final Random rnd = ThreadLocalRandom.current();
    private final Gson gson = new Gson();
    private final String trackerUrl = "/api/tracker";
    private final String signupUrl = "/api/developers/signup";
    private final String registerUrl = "/api/applications/register";
    private final DataRecord[] records = Stream.generate(DataRecordMother::createRecord).limit(4).toArray(DataRecord[]::new);
    private final DevProfile alice = DevProfileMother.alice();
    private final DevProfile aliceCopy = DevProfileMother.alice();
    private final DevProfile bob = DevProfileMother.bob();
    private final AppProfile demo1 = AppProfileMother.demo1();
    private final AppProfile demo1Copy = AppProfileMother.demo1();
    private final AppProfile demo2 = AppProfileMother.demo2();
    private final AppProfile basicApp = AppProfileMother.basicApp();

    public FitnessTrackerTest() {
        super("../fitness_db.mv.db");
    }

    CheckResult testPostTracker(DataRecord[] data, AppProfile appProfile) {
        for (DataRecord item : data) {
            HttpResponse response = post(trackerUrl, gson.toJson(item))
                    .addHeader("X-API-Key", appProfile.getApikey())
                    .send();
            checkStatusCode(response, 201);

            item.setApplication(appProfile.getName());
        }
        return CheckResult.correct();
    }

    CheckResult testPostTrackerUnauthenticated(DataRecord data, AppProfile appProfile) {
        HttpRequest request = post(trackerUrl, gson.toJson(data));
        if (appProfile.getApikey() != null) {
            request = request.addHeader("X-API-Key", appProfile.getApikey());
        }
        HttpResponse response = request.send();
        checkStatusCode(response, 401);

        return CheckResult.correct();
    }

    CheckResult testGetTracker(DataRecord[] data, AppProfile appProfile) {
        HttpResponse response = get(trackerUrl)
                .addHeader("X-API-Key", appProfile.getApikey())
                .send();

        checkStatusCode(response, 200);
        checkDataJson(response, data);

        return CheckResult.correct();
    }

    CheckResult testGetTrackerUnauthenticated(AppProfile appProfile) {
        HttpRequest request = get(trackerUrl);
        if (appProfile.getApikey() != null) {
            request = request.addHeader("X-API-Key", appProfile.getApikey());
        }
        HttpResponse response = request.send();
        checkStatusCode(response, 401);

        return CheckResult.correct();
    }

    CheckResult testRegisterValidDev(DevProfile devProfile) {
        HttpResponse response = post(signupUrl, gson.toJson(devProfile)).send();

        checkStatusCode(response, 201);

        String location = response.getHeaders().get("Location");
        if (location == null || !location.matches("/api/developers/.+")) {
            return CheckResult.wrong(
                    "User registration response should contain the 'Location' header" +
                            " with the value '/api/developers/<id>'"
            );
        }

        devProfile.setId(location.replaceAll(".+/", ""));

        return CheckResult.correct();
    }

    CheckResult testRegisterInvalidDev(DevProfile devProfile) {
        HttpResponse response = post(signupUrl, gson.toJson(devProfile)).send();

        checkStatusCode(response, 400);

        return CheckResult.correct();
    }

    CheckResult testRegisterApp(DevProfile devProfile, AppProfile appProfile) {
        HttpResponse response = post(registerUrl, gson.toJson(appProfile))
                .basicAuth(devProfile.getEmail(), devProfile.getPassword())
                .send();

        checkStatusCode(response, 201);
        checkAppRegistrationResponseJson(response, appProfile);

        var apikey = response.getJson().getAsJsonObject().get("apikey").getAsString();
        appProfile.setApikey(apikey);
        if (devProfile.getApplications() == null) {
            devProfile.setApplications(new ArrayList<>());
        }
        devProfile.getApplications().add(appProfile);

        return CheckResult.correct();
    }

    CheckResult testRegisterInvalidApp(DevProfile devProfile,
                                       AppProfile appProfile) {
        HttpResponse response = post(registerUrl, gson.toJson(appProfile))
                .basicAuth(devProfile.getEmail(), devProfile.getPassword())
                .send();

        checkStatusCode(response, 400);

        return CheckResult.correct();
    }

    CheckResult testGetProfile(DevProfile devProfile,
                               DevProfile anotherDevProfile) {
        var profileUrl = "/api/developers/" + devProfile.getId();

        // no auth
        HttpResponse response = get(profileUrl).send();
        checkStatusCode(response, 401);

        // bad credentials
        response = get(profileUrl)
                .basicAuth(devProfile.getEmail(), devProfile.getPassword() + "12345")
                .send();
        checkStatusCode(response, 401);

        // wrong user
        response = get(profileUrl)
                .basicAuth(anotherDevProfile.getEmail(), anotherDevProfile.getPassword())
                .send();
        checkStatusCode(response, 403);

        // proper user
        response = get(profileUrl)
                .basicAuth(devProfile.getEmail(), devProfile.getPassword())
                .send();
        checkStatusCode(response, 200);
        checkProfileJson(response, devProfile);

        return CheckResult.correct();
    }

    CheckResult testRateLimit(DataRecord data, AppProfile appProfile) {
        HttpRequest getRequest = get(trackerUrl)
                .addHeader("X-API-Key", appProfile.getApikey());
        HttpRequest postRequest = post(trackerUrl, gson.toJson(data))
                .addHeader("X-API-Key", appProfile.getApikey());

        int rejectedRequests = countRejectedRequests(postRequest, getRequest);
        if (rejectedRequests != 3) {
            return CheckResult.wrong(
                    "Too few rejected requests for a basic application within 1 second.\n" +
                            "Expected 3 responses with the status code 429 but received only %d such responses"
                                    .formatted(rejectedRequests)
            );
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            System.out.println("Failed to sleep for 1200 ms");
        }

        rejectedRequests = countRejectedRequests(postRequest, getRequest);
        if (rejectedRequests != 3) {
            return CheckResult.wrong(
                    "Too few rejected requests for a basic application within 1 second.\n" +
                            "Expected 3 responses with the status code 429 but received only %d such responses"
                                    .formatted(rejectedRequests)
            );
        }

        return CheckResult.correct();
    }

    private void checkStatusCode(HttpResponse response, int expected) {
        var actual = response.getStatusCode();
        var method = response.getRequest().getMethod();
        var endpoint = response.getRequest().getEndpoint();
        if (actual != expected) {
            throw new WrongAnswer("""
                    %s %s should respond with status code %d, responded with %d
                    \r
                    """.formatted(method, endpoint, expected, actual));
        }
    }

    private void checkDataJson(HttpResponse response, DataRecord[] expectedData) {
        expect(response.getContent()).asJson().check(
                isArray(expectedData.length)
                        .item(isObject()
                                .value("id", any())
                                .value("username", expectedData[3].getUsername())
                                .value("activity", expectedData[3].getActivity())
                                .value("duration", expectedData[3].getDuration())
                                .value("calories", expectedData[3].getCalories())
                                .value("application", expectedData[3].getApplication())
                        )
                        .item(isObject()
                                .value("id", any())
                                .value("username", expectedData[2].getUsername())
                                .value("activity", expectedData[2].getActivity())
                                .value("duration", expectedData[2].getDuration())
                                .value("calories", expectedData[2].getCalories())
                                .value("application", expectedData[2].getApplication())
                        )
                        .item(isObject()
                                .value("id", any())
                                .value("username", expectedData[1].getUsername())
                                .value("activity", expectedData[1].getActivity())
                                .value("duration", expectedData[1].getDuration())
                                .value("calories", expectedData[1].getCalories())
                                .value("application", expectedData[1].getApplication())
                        )
                        .item(isObject()
                                .value("id", any())
                                .value("username", expectedData[0].getUsername())
                                .value("activity", expectedData[0].getActivity())
                                .value("duration", expectedData[0].getDuration())
                                .value("calories", expectedData[0].getCalories())
                                .value("application", expectedData[0].getApplication())
                        )
        );
    }

    private void checkProfileJson(HttpResponse response, DevProfile expectedData) {
        var applications = expectedData.getApplications();

        expect(response.getContent()).asJson().check(
                isObject()
                        .value("id", any())
                        .value("email", Pattern.compile(expectedData.getEmail(), Pattern.CASE_INSENSITIVE))
                        .value("applications", isArray(expectedData.getApplications().size())
                                .item(isObject()
                                        .value("id", any())
                                        .value("name", applications.get(1).getName())
                                        .value("description", applications.get(1).getDescription())
                                        .value("apikey", applications.get(1).getApikey())
                                        .value("category", applications.get(1).getCategory())
                                )
                                .item(isObject()
                                        .value("id", any())
                                        .value("name", applications.get(0).getName())
                                        .value("description", applications.get(0).getDescription())
                                        .value("apikey", applications.get(0).getApikey())
                                        .value("category", applications.get(0).getCategory())
                                )
                        )
        );
    }

    private void checkAppRegistrationResponseJson(HttpResponse response, AppProfile expectedData) {
        expect(response.getContent()).asJson().check(
                isObject()
                        .value("name", expectedData.getName())
                        .value("apikey", isString())
                        .value("category", expectedData.getCategory())
        );
    }

    CheckResult reloadServer() {
        try {
            reloadSpring();
        } catch (Exception ex) {
            throw new WrongAnswer("Failed to restart application: " + ex.getMessage());
        }
        return CheckResult.correct();
    }

    private int countRejectedRequests(HttpRequest... requests) {
        assert requests.length >= 2;

        int rejectedRequests = 0;
        var statusCode = requests[0].send().getStatusCode();
        if (statusCode == 429) {
            rejectedRequests++;
        }
        for (int i = 0; i < 3; i++) {
            statusCode = requests[1].send().getStatusCode();
            if (statusCode == 429) {
                rejectedRequests++;
            }
        }

        // retry in case bucket was replenished during the test
        if (rejectedRequests < 3) {
            rejectedRequests = 0;
            for (int i = 0; i < 3; i++) {
                statusCode = requests[1].send().getStatusCode();
                if (statusCode == 429) {
                    rejectedRequests++;
                }
            }
        }

        return rejectedRequests;
    }

    @DynamicTest
    DynamicTesting[] dt = new DynamicTesting[]{
            () -> testRegisterValidDev(alice),
            () -> testRegisterValidDev(bob),
            () -> testRegisterInvalidDev(DevProfileMother.withBadEmail(null)),
            () -> testRegisterInvalidDev(DevProfileMother.withBadEmail("")),
            () -> testRegisterInvalidDev(DevProfileMother.withBadEmail("email")),
            () -> testRegisterInvalidDev(DevProfileMother.withBadPassword(null)),
            () -> testRegisterInvalidDev(DevProfileMother.withBadPassword("")),
            () -> testRegisterInvalidDev(aliceCopy),
            () -> testRegisterApp(alice, demo1),
            () -> testRegisterApp(alice, demo2),
            () -> testRegisterApp(bob, basicApp),
            () -> testRegisterInvalidApp(alice, demo1Copy),
            () -> testRegisterInvalidApp(alice, AppProfileMother.withBadName(null)),
            () -> testRegisterInvalidApp(alice, AppProfileMother.withBadName(" ")),
            () -> testRegisterInvalidApp(alice, AppProfileMother.withBadDescription(null)),
            () -> testGetProfile(alice, bob),
            () -> testPostTracker(records, rnd.nextBoolean() ? demo1 : demo2),
            () -> testPostTrackerUnauthenticated(DataRecordMother.createRecord(),
                    AppProfileMother.withBadApiKey(null)),
            () -> testPostTrackerUnauthenticated(DataRecordMother.createRecord(),
                    AppProfileMother.withBadApiKey(UUID.randomUUID().toString())),
            () -> testGetTracker(records, rnd.nextBoolean() ? demo1 : demo2),
            () -> testGetTrackerUnauthenticated(AppProfileMother.withBadApiKey(null)),
            () -> testGetTrackerUnauthenticated(AppProfileMother.withBadApiKey(UUID.randomUUID().toString())),
            this::reloadServer,
            () -> testGetTracker(records, rnd.nextBoolean() ? demo1 : demo2),
            () -> testRateLimit(DataRecordMother.createRecord(), basicApp),
    };
}
