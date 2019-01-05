using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;

namespace TcpMessageQueue.model
{
    class Response
    {
        public int Status { get; set; }
        public Dictionary<String, String> Headers { get; set; }
        public JObject Payload { get; set; }
    }
}
