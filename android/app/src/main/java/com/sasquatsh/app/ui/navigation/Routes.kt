package com.sasquatsh.app.ui.navigation

sealed class Routes(val route: String) {
    // Auth
    data object Login : Routes("login")
    data object Signup : Routes("signup")
    data object ForgotPassword : Routes("forgot-password")

    // Main tabs
    data object Dashboard : Routes("dashboard")
    data object Events : Routes("events")
    data object Groups : Routes("groups")
    data object More : Routes("more")

    // Events
    data object EventDetail : Routes("events/{eventId}") {
        fun withId(id: String) = "events/$id"
    }
    data object CreateEvent : Routes("events/create")
    data object EditEvent : Routes("events/{eventId}/edit") {
        fun withId(id: String) = "events/$id/edit"
    }

    // Groups
    data object GroupDetail : Routes("groups/{slug}") {
        fun withSlug(slug: String) = "groups/$slug"
    }
    data object CreateGroup : Routes("groups/create")

    // Planning
    data object PlanGameNight : Routes("groups/{slug}/plan") {
        fun withSlug(slug: String) = "groups/$slug/plan"
    }
    data object PlanningSession : Routes("planning/{sessionId}") {
        fun withId(id: String) = "planning/$id"
    }

    // LFP
    data object LookingForPlayers : Routes("looking-for-players")

    // MTG
    data object MtgDecks : Routes("mtg/decks")
    data object MtgDeckBuilder : Routes("mtg/decks/{deckId}") {
        fun withId(id: String) = "mtg/decks/$id"
    }

    // Profile
    data object Profile : Routes("profile")
    data object Billing : Routes("billing")

    // Invitations
    data object GroupInvite : Routes("groups/invite/{code}") {
        fun withCode(code: String) = "groups/invite/$code"
    }
    data object EventInvite : Routes("invite/{code}") {
        fun withCode(code: String) = "invite/$code"
    }
}
