export const TRANSLATION = {
    General: {
        Grid: "Net",
        GridBuy: "Netafname",
        GridSell: "Netteruglevering",
        Production: "Opwekking",
        Consumption: "Verbruik",
        Power: "Vermogen",
        StorageSystem: "Batterij",
        History: "Historie",
        NoValue: "Geen waarde",
        Soc: "Laadstatus",
        Percentage: "Procent",
        More: "Meer…",
        ChargePower: "Laad vermogen",
        DischargePower: "Ontlaad vermogen",
        PeriodFromTo: "van {{value1}} tot {{value2}}", // value1 = beginning date, value2 = end date
        DateFormat: "DD-MM-YYYY", // e.g. German: DD.MM.YYYY, English: YYYY-MM-DD (DD = Day, MM = Month, YYYY = Year)
        Week: {
            Monday: "Maandag",
            Tuesday: "Dinsdag",
            Wednesday: "Woensdag",
            Thursday: "Donderdag",
            Friday: "Vrijdag",
            Saturday: "Zaterdag",
            Sunday: "Zondag"
        }
    },
    Menu: {
        Overview: "Overzicht",
        AboutUI: "Over FEMS- UI"
    },
    Overview: {
        AllConnected: "Alle verbindingen gemaakt.",
        ConnectionSuccessful: "Succesvol verbonden met {{value }}.", // (value = Name vom Websocket)
        ConnectionFailed: "Verbinding met {{ value } } mislukt.", // (value = Name vom Websocket)
        ToEnergymonitor: "Naar Energiemonitor...",
        IsOffline: "FEMS is offline!"
    },
    Device: {
        Overview: {
            Energymonitor: {
                Title: "Energiemonitor",
                ConsumptionWarning: "Verbruik & onbekende producenten",
                Storage: "Batterij",
                ReactivePower: "Blind vermogen",
                ActivePower: "Actief vermogen",
                GridMeter: "Energiemeter",
                ProductionMeter: "Productiemeter"
            },
            Energytable: {
                Title: "Energie tabel"
            }
        },
        History: {
            SelectedPeriod: "Geselecteerde periode: ",
            OtherPeriod: "Andere periode: ",
            Period: "Periode",
            Today: "Vandaag",
            Yesterday: "Gisteren",
            LastWeek: "Vorige week",
            LastMonth: "Vorige maand",
            LastYear: "Vorig jaar",
            Go: "Ga!"
        },
        Config: {
            Overview: {
                Bridge: "Verbindingen en apparaten",
                Scheduler: "Toepassingsschema",
                Controller: "Toepassingen",
                Simulator: "Simulator",
                ExecuteSimulator: "Simulatie uitvoeren",
                Log: "Log",
                LiveLog: "Live System log",
                ManualControl: "Handmatige bediening"
            },
            More: {
                ManualCommand: "Handmatig commando",
                Send: "Verstuur",
                RefuInverter: "REFU inverter",
                RefuStartStop: "Inverter starten/ stoppen",
                RefuStart: "Start",
                RefuStop: "Stop",
                ManualpqPowerSpecification: "Gespecificeerd vermogen",
                ManualpqSubmit: "Toepassen",
                ManualpqReset: "Reset"
            },
            Scheduler: {
                NewScheduler: "New Schema...",
                Class: "Soort: ",
                NotImplemented: "Gegevens niet geïmplementeerd: ",
                Contact: "Dit zou niet mogen gebeuren.Neem contact op met <a href=\"mailto:{{value}}\">{{value}}</a>.", // (value = E - Mail vom FEMS- Team)
                Always: "Altijd"
            },
            Log: {
                AutomaticUpdating: "Automatisch updaten",
                Timestamp: "Tijdspit",
                Level: "Niveau",
                Source: "Bron",
                Message: "Bericht"
            },
            Controller: {
                InternallyID: "Intern ID:",
                App: "App:",
                Priority: "Prioriteit: "
            },
            Bridge: {
                NewDevice: "Nieuw apparaat…",
                NewConnection: "Nieuwe verbinding..."
            }
        }
    },
    About: {
        UI: "Gebruikersinterface voor FEMS en OpenEMS",
        Developed: "Deze gebruikersinterface is ontwikkeld door FENECON als open- source - software.",
        Fenecon: "Meer over FENECON",
        Fems: "Meer over FEMS",
        Sourcecode: "Broncode",
        CurrentDevelopments: "Huidige ontwikkelingen",
        Build: "Versie",
        Contact: "Voor meer informatie of suggesties over het systeem, neem contact op met het FEMS team via <a href=\"mailto:{{value}}\">{{value}}</a>.", // (value = E - Mail vom FEMS- Team)
        Language: "Selecteer taal: "
    },
    Notifications: {
        Failed: "Verbinding mislukt.",
        LoggedInAs: "Aangemeld als gebruiker {{ value } }.", // (value = Benutzername)
        LoggedIn: "Aangemeld.",
        AuthenticationFailed: "Geen verbinding.Autorisatie mislukt.",
        Closed: "Verbinding beëindigd."
    }
}