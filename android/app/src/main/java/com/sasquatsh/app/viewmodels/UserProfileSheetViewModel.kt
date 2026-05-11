package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import com.sasquatsh.app.services.BadgesService
import com.sasquatsh.app.services.CollectionsService
import com.sasquatsh.app.services.ProfileService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserProfileSheetViewModel @Inject constructor(
    val profileService: ProfileService,
    val badgesService: BadgesService,
    val collectionsService: CollectionsService
) : ViewModel()
