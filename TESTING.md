# Testing Guide
This guide is intended to layout the general automated test strategy for OpenLMIS.

## Test Strategy
OpenLMIS, like many software projects, relies on testing to guide development and prevent regressions.  To effect this we've adopted a standard set of tools to write and execute our tests, and categorize them to understand what types of tests we have, who writes them, when they're written, run, and where they live.


## Types of Tests
The following test categories have been identified for use in OpenLMIS.  As illustrated in this great [slide deck](http://martinfowler.com/articles/microservice-testing/), we expect the effort/number of tests in each category to reflect the [test pyramid](http://martinfowler.com/articles/microservice-testing/#conclusion-test-pyramid):

1. [Unit](#unit)
2. [Integration](#integration)
3. [Contract](#contract)
4. [End-to-End](#e2e)

### Unit tests <a name="unit"></a>

* Who:  written by code-author during implementation
* What: the smallest unit (e.g. one piece of a model's behavior, a function, etc)
* When: at build time, should be /fast/ and targetted - I can run just a portion of the test suite
* Where: Reside inside a service, next to unit under test. Generally able to access package-private scope
* Why: to test fundamental pieces/functionality, helps guide and document design and refactors, protects against regression

#### Unit test examples

* __Every single test should be independent and isolated. Unit test shouldn't depend on another unit test.__

  DO NOT:
  ```java
  List<Item> list = new ArrayList<>();

  @Test
  public void testListSize1() {
    Item item = new Item();
    list.add(item);
    assertEquals(1, list.size());
  }

  @Test
  public void testListSize2() {
    Item item = new Item();
    list.add(item);
    assertEquals(2, list.size());
  }
  ```
* __One behavior should be tested in just one unit test.__

  DO NOT:
  ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18AndIsPersonAbleToRunForPresident() {
    int age = 17;
    boolean isAdult = ageService.isAdult(age);
    assertFalse(isAdult);

    boolean isAbleToRunForPresident = electionsService.isAbleToRunForPresident(age)
    assertFalse(isAbleToRunForPresident);
  }
  ```
  DO:
    ```java

  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    int age = 17;
    boolean isAdult = ageService.isAdult(age);
    assertFalse(isAdult);
  }

  @Test
  public void testIsPersonAbleToRunForPresident() {
    int age = 17;
    boolean isAbleToRunForPresident = electionsService.isAbleToRunForPresident(age)
    assertFalse(isAbleToRunForPresident);
  }
  ```
* __Every unit test should have at least one assertion.__

  DO NOT:
  ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    int age = 17;
    boolean isAdult = ageService.isAdult(age);
  }
  ```

  DO:
  ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    int age = 17;
    boolean isAdult = ageService.isAdult(age);
    assertFalse(isAdult);
  }
  ```
* __Don't make unnecessary assertions. Don't assert mocked behavior, avoid assertions that check the exact same thing as another unit test.__

  DO NOT:
 ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    int age = 17;
    assertEquals(17, age);

    boolean isAdult = ageService.isAdult(age);
    assertFalse(isAdult);
  }
  ```
* __Unit test has to be independent from external resources (i.e. don't connect with databases or servers)__

  DO NOT:
 ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    String uri = String.format("http://127.0.0.1:8080/age/", HOST, PORT);
    HttpPost httpPost = new HttpPost(uri);
    HttpResponse response = getHttpClient().execute(httpPost);
    assertEquals(HttpStatus.ORDINAL_200_OK, response.getStatusLine().getStatusCode());
  }
  ```
* __Unit test shouldn't test Spring Contexts. Integration tests are better for this purpose.__

  DO NOT:
 ```java
  @RunWith(SpringJUnit4ClassRunner.class)
  @ContextConfiguration(locations = {"/services-test-config.xml"})
  public class MyServiceTest implements ApplicationContextAware
  {

    @Autowired
    MyService service;
    ...
      @Override
      public void setApplicationContext(ApplicationContext context) throws BeansException
      {
          // something with the context here
      }
  }
  ```
* __Test method name should consistently show what is being tested.__

  DO:
  ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    ...
  }
  ```
  DO NOT:
  ```java

  @Test
  public void firstTests() {
    ...
  }
  ```
* __Unit test should be repeatable - each run should yield the same result.__

  DO NOT:
  ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    int age = randomGenerator.nextInt(100);
    boolean isAdult = ageService.isAdult(age);
    assertFalse(isAdult);
  }
  ```
* __You should remember about intializing and cleaning each global state between test runs.__

  DO:
  ```java
  @Mock
  private AgeService ageService;
  private age;

  @Before
  public void init() {
    age = 18;
    when(ageService.isAdult(age)).thenReturn(true);
  }

  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    boolean isAdult = ageService.isAdult(age);
    assertTrue(isAdult);
  }
  ```
* __Test should run fast. When we have hundreds of tests we just don't want to wait several minutes till all tests pass.__

  DO NOT:
 ```java
  @Test
  public void testIsNotAnAdultIfAgeLessThan18() {
    int age = 17;
    sleep(1000);
    boolean isAdult = ageService.isAdult(age);
    sleep(1000);
    assertFalse(isAdult);
  }
  ```

### Integration Testing <a name="integration"></a>

* Who: Code author during implementation
* What: Test basic operation of a service to persistent storage or a service to another service.  When another service is required, a test-double should be used, not the actual service.
* When: As explicitly asked for, these tests are typically slower and therefore need to be kept seperate from build to not slow development.  Will be run in CI on every change.
* Where: Reside inside a service, seperated from other types of tests/code.
* Why:  Ensures that the basic pathways to a service's external run-time dependancies work.  e.g. that a db schema supports the ORM, or a non-responsive service call is gracefully handled.

### Contract <a name="contract"></a>

* Who: Code author during implementation, with input from BA/QA.
* What: Enforces contracts between and to services.
* When: Ran in CI.
* Where: Reside inside seperate repository:  [openlmis-contract-tests](http://github.com/openlmis/openlmis-contract-tests).
* Why:  Tests multiple services working together, testing contracts that a Service both provides as well as the requirements a dependant has. 

### End-to-End <a name="e2e"></a>

* Who: QA / developer with input from BA.
* What: Typical/core business scenarios.
* When: Ran in CI.
* Where: Resides in seperate repository.
* Why: Ensures all the pieces are working together to carry-out a business scenario.  Helps ensure end-users can achieve their goals.

## Testing services dependent on external APIs
OpenLMIS is using WireMock for mocking web services. An example integration test can be found here:
https://github.com/OpenLMIS/openlmis-example/blob/master/src/test/java/org/openlmis/example/WeatherServiceTest.java

The stub mappings which are served by WireMock's HTTP server are placed under _src/test/resources/mappings_ and _src/test/resources/__files_
For instructions on how to create them please refer to http://wiremock.org/record-playback.html
