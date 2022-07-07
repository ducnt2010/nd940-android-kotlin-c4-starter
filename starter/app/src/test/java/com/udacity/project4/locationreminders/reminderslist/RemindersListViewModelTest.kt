package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import org.hamcrest.MatcherAssert.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
//@Config(sdk = [28])
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var reminderDataSource: FakeDataSource

    private var remindersDummy = mutableListOf<ReminderDTO>(
        ReminderDTO("tile1", "des1", "location1", 12.345, 34.567),
        ReminderDTO("tile2", "des2", "location2", 12.345, 34.567)
    )

    @Before
    fun setupViewModel() {
        stopKoin()
        reminderDataSource = FakeDataSource(remindersDummy)
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @Test
    fun loadReminders_loading() = runBlockingTest {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // WHEN: Load reminder
        remindersListViewModel.loadReminders()

        // THEN
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()
        // Then assert that the progress indicator is hidden.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_success() = runBlockingTest {
        // WHEN
        remindersListViewModel.loadReminders()

        // THEN
        val remindersList = remindersListViewModel.remindersList.getOrAwaitValue()
        assertThat(remindersList, `is`(notNullValue()))
        assertThat(remindersList.size, `is`(remindersDummy.size))
    }

    @Test
    fun loadReminders_error() = runBlockingTest {
        reminderDataSource.setReturnError(true)

        remindersListViewModel.loadReminders()

        val snackBarMessage = remindersListViewModel.showSnackBar.getOrAwaitValue()
        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()

        assertThat(snackBarMessage, `is`("Test getReminders exception"))
        assertThat(showNoData, `is`(true))
    }

}
