package org.cloudfoundry.identity.uaa.login.feature;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.cloudfoundry.identity.uaa.login.test.IntegrationTestRule;
import org.cloudfoundry.identity.uaa.login.test.TestClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.security.SecureRandom;
import java.util.Iterator;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class ChangeEmailIT {

    @Autowired @Rule
    public IntegrationTestRule integrationTestRule;

    @Autowired
    WebDriver webDriver;

    @Value("${integration.test.base_url}")
    String baseUrl;

    @Autowired
    SimpleSmtpServer simpleSmtpServer;

    @Autowired
    TestClient testClient;

    private String userEmail;
    @Before
    public void setUp() throws Exception {
        int randomInt = new SecureRandom().nextInt();

        String adminAccessToken = testClient.getOAuthAccessToken("admin", "adminsecret", "client_credentials", "clients.read clients.write clients.secret");

        String scimClientId = "scim" + randomInt;
        testClient.createScimClient(adminAccessToken, scimClientId);

        String scimAccessToken = testClient.getOAuthAccessToken(scimClientId, "scimsecret", "client_credentials", "scim.read scim.write password.write");

        userEmail = "user" + randomInt + "@example.com";
        testClient.createUser(scimAccessToken, userEmail, userEmail, "secret");
    }

    @Test
    public void testChangeEmail() throws Exception {
        signIn(userEmail, "secret");
        int receivedEmailSize = simpleSmtpServer.getReceivedEmailSize();

        webDriver.get(baseUrl + "/change_email");

        assertEquals("Current Email Address: " + userEmail, webDriver.findElement(By.cssSelector(".email-display")).getText());
        String newEmail = userEmail.replace("user", "new");
        webDriver.findElement(By.name("newEmail")).sendKeys(newEmail);
        webDriver.findElement(By.xpath("//input[@value='Send Verification Link']")).click();

        assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Instructions Sent"));
        assertEquals(receivedEmailSize + 1, simpleSmtpServer.getReceivedEmailSize());

        Iterator receivedEmail = simpleSmtpServer.getReceivedEmail();
        SmtpMessage message = (SmtpMessage) receivedEmail.next();
        receivedEmail.remove();

        assertEquals(newEmail, message.getHeaderValue("To"));
        assertThat(message.getBody(), containsString("Verify your email"));

        String link = testClient.extractLink(message.getBody());
        webDriver.get(link);

        assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Account Settings"));
        assertThat(webDriver.findElement(By.cssSelector(".alert-success")).getText(), containsString("Email address successfully verified."));
        assertThat(webDriver.findElement(By.cssSelector(".nav")).getText(), containsString(newEmail));
        assertThat(webDriver.findElement(By.cssSelector(".profile")).getText(), containsString(newEmail));
    }

    private void signIn(String userName, String password) {
        webDriver.get(baseUrl + "/login");
        webDriver.findElement(By.name("username")).sendKeys(userName);
        webDriver.findElement(By.name("password")).sendKeys(password);
        webDriver.findElement(By.xpath("//input[@value='Sign in']")).click();
        assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));
    }
}
