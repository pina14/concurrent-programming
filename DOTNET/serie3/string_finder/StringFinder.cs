using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace string_finder
{
    class StringFinder
    {
        private CancellationTokenSource cts;
        private Action<String, String, String> ActionWhenFound;

        /// <summary>
        /// Receives Action to perform each time the string is found in one file
        /// </summary>
        public StringFinder(Action<String, String, String> cb)
        {
            ActionWhenFound = cb;
        }

        public async Task PerformSearch(String dir, String textToSearch)
        {
            cts = new CancellationTokenSource();
            String[] files = Directory.GetFiles(dir);
            Task[] tasks = new Task[files.Length];
            ParallelOptions options = new ParallelOptions { CancellationToken = cts.Token };
            Parallel.ForEach(files, options, (curFile, loopState, index) => {
                options.CancellationToken.ThrowIfCancellationRequested();
                tasks[index] = SearchFile(curFile, textToSearch);
            });

            foreach (Task t in tasks)
                await t;
        }

        /**
         * Using StreamReader instead of Parallel loop, because with Parallel loop we would have to read all the lines
         * before starting to analyse them, what could cause an OutOfMemoryException or BufferOverflow.
         **/
        private async Task SearchFile(String filePath, String textToSearch)
        {
            String fileName = Path.GetFileName(filePath);
            int idx = 1;
            using (StreamReader reader = new StreamReader(filePath, true))
            {
                while (!reader.EndOfStream)
                {
                    if (cts.IsCancellationRequested)
                        throw new OperationCanceledException();

                    String line = await reader.ReadLineAsync();
                    SearchLine(textToSearch, fileName, line, idx++);
                }
            }
        }

        private void SearchLine(String textToSearch, String fileName, String line, int idx)
        {
            if (line.Contains(textToSearch))
                ActionWhenFound(fileName, idx.ToString(), line);
        }

        public void CancelSearch()
        {
            cts.Cancel();
        }

        public void DisposeCTS()
        {
            cts.Dispose();
        }
    }
}
