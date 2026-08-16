package com.itemrespawntimer.timermodel;

public enum RemoveTimerEvent {
    CAN_SEE_LOCATION { //todo: implement this option
        @Override
        public String toString()
        {
            return "you can see the location";
        }
    },
    SAME_WORLD { //todo: implement this option
        @Override
        public String toString()
        {
            return "you're in the same world";
        }
    },
    TWICE_RESPAWN_TIME { //todo: implement this option
        @Override
        public String toString()
        {
            return "after twice the respawn time";
        }
    },
    T_PLUS_60_SECONDS { //todo: implement this option
        @Override
        public String toString()
        {
            return "at T+60 seconds";
        }
    },
    T_PLUS_X { //todo: implement this option
        @Override
        public String toString()
        {
            return "at T+X (configured below)";
        }
    };
}
