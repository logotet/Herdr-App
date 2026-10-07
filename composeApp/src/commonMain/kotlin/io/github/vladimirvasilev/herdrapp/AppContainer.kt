package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.TerminalRepository

/** What the UI needs from the platform. Built once per process and handed to ViewModels. */
class AppContainer(
    val session: SessionRepository,
    val terminal: TerminalRepository,
    val hosts: HostRepository,
    val settings: SettingsRepository,
)
