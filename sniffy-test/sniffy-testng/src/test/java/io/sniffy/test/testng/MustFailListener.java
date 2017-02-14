package io.sniffy.test.testng;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * In TestNG exception expectations are checked before the listeners, so we cannot use this functionality
 * for testing the Sniffer as we do in JUnit
 */
public class MustFailListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {

    }

    @Override
    public void onTestSuccess(ITestResult result) {
        failIfRequired(result);

    }

    private void failIfRequired(ITestResult result) {
        MustFail mustFail = result.getMethod().getConstructorOrMethod().getMethod().getAnnotation(MustFail.class);

        if (null != mustFail) {
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(new AssertionError("This test should have failed with WrongNumberOfQueriesError exception"));
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {

        MustFail mustFail = result.getMethod().getConstructorOrMethod().getMethod().getAnnotation(MustFail.class);

        if (null != mustFail) {
            if (result.getStatus() == ITestResult.FAILURE &&
                    null != result.getThrowable() &&
                    mustFail.value().isAssignableFrom(result.getThrowable().getClass())) {
                result.setStatus(ITestResult.SUCCESS);
                result.setThrowable(null);
                return;
            }
        }

        result.setStatus(ITestResult.FAILURE);
        result.setThrowable(new AssertionError("This test should have failed with WrongNumberOfQueriesError exception"));

    }

    @Override
    public void onTestSkipped(ITestResult result) {
        failIfRequired(result);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        failIfRequired(result);
    }

    @Override
    public void onStart(ITestContext context) {

    }

    @Override
    public void onFinish(ITestContext context) {

    }
}
