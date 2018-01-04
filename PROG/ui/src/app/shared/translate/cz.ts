export const TRANSLATION = {
    General: {
        Grid: "Síť",
        GridBuy: "Nákup ze sítě",
        GridSell: "Prodej do sítě",
        Production: "Výroba",
        Consumption: "Spotřeba",
        Power: "Výkon",
        StorageSystem: "Systém bateriového úložiště",
        History: "Historie",
        NoValue: "Žádná hodnota",
        Soc: "Stav nabití",
        Percentage: "Procentuální vyjádření",
        More: "Další",
        ChargePower: "Nabíjecí výkon",
        DischargePower: "Vybíjecí výkon",
        PeriodFromTo: "od {{value1}} do {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "DD.MM.YYYY", // e.g. German: DD.MM.YYYY, English: YYYY-MM-DD (DD = Day, MM = Month, YYYY = Year)
        Week: {
            Monday: "Pondělí",
            Tuesday: "Úterý",
            Wednesday: "Středa",
            Thursday: "Čtvrte",
            Friday: "Pátek",
            Saturday: "Sobota",
            Sunday: "Neděle"
        }
    },
    Menu: {
        Overview: "Přehled",
        AboutUI: "About FEMS-UI"
    },
    Overview: {
        AllConnected: "Všechna připojení aktivní.",
        ConnectionSuccessful: "Úspěšně připojeno k {{value}}.", // value = name of websocket
        ConnectionFailed: "Připojení k {{value}} selhalo.", // value = name of websocket
        ToEnergymonitor: "Do Monitoringu energetických toků…",
        IsOffline: "FEMS je ve stavu offline!"
    },
    Device: {
        Overview: {
            Energymonitor: {
                Title: "Monitoring energetických toků",
                ConsumptionWarning: "Spotřeba & neznámá výroba",
                Storage: "Úložiště",
                ReactivePower: "Jalový výkon",
                ActivePower: "Činný výkon",
                GridMeter: "Elektroměr - Odběr",
                ProductionMeter: "Elektroměr - Výroba"
            },
            Energytable: {
                Title: "Tabulka hodnot"
            }
        },
        History: {
            SelectedPeriod: "Zvolené období: ",
            OtherPeriod: "Další období:",
            Period: "Období",
            Today: "Dnes",
            Yesterday: "Včera",
            LastWeek: "Poslední týden",
            LastMonth: "Poslední měsíc",
            LastYear: "Poslední rok",
            Go: "Jdi!"
        },
        Config: {
            Overview: {
                Bridge: "Připojená zařízení",
                Scheduler: "Plánovač aplikací",
                Controller: "Aplikace",
                Simulator: "Simulátor",
                ExecuteSimulator: "Zahájit simulaci",
                Log: "Log",
                LiveLog: "Live log systému",
                ManualControl: "Manuální ovládání",
            },
            More: {
                ManualCommand: "Manuální příkaz ",
                Send: "Odeslat",
                RefuInverter: "REFU Střídač",
                RefuStartStop: "Start/Stop střídače",
                RefuStart: "Start",
                RefuStop: "Stop",
                ManualpqPowerSpecification: "Specifikace výkonu",
                ManualpqSubmit: "Zadání",
                ManualpqReset: "Reset"
            },
            Scheduler: {
                NewScheduler: "Nový plánovač...",
                Class: "Třída:",
                NotImplemented: "Zadání nebylo implementováno: ",
                Contact: "Došlo k chybě. Prosím kontaktujte <a href=\"mailto:{{value}}\">{{value}}</a>.", // value = Mail from FEMS-Team
                Always: "Vždy"
            },
            Log: {
                AutomaticUpdating: "Automatický update",
                Timestamp: "Časové razítko",
                Level: "Úroveň",
                Source: "Zdroj",
                Message: "Zpráva"
            },
            Controller: {
                InternallyID: "Vnitřní ID:",
                App: "App:",
                Priority: "Priorita:"
            },
            Bridge: {
                NewDevice: "Nové zařízení...",
                NewConnection: "Nové připojení..."
            }
        }
    },
    About: {
        UI: "Uživatelské rozhraní pro FEMS a OpenEMS",
        Developed: "Toto uživatelské rozhraní bylo vyvinuto společností FENECON jako open-source software.",
        Fenecon: "Více o společnosti FENECON",
        Fems: "Více o FEMS",
        Sourcecode: "Zdrojový kód",
        CurrentDevelopments: "Aktuální vývoj",
        Build: "Aktuální verze",
        Contact: "S případnými návrhy a pro další informace k systému prosím kontaktujte náš FEMS tým na <a href=\"mailto:{{value}}\">{{value}}</a>.", // value = Mail from FEMS-Team
        Language: "Zvolte jazyk:"
    },
    Notifications: {
        Failed: "Připojení selhalo.",
        LoggedInAs: "Uživatel přihlášen jako {{value}}.", // value = username
        LoggedIn: "Přihlášení proběhlo úspěšně.",
        AuthenticationFailed: "Žádné připojení: Ověření uživatele selhalo.",
        Closed: "Připojení ukončeno."
    }
}
