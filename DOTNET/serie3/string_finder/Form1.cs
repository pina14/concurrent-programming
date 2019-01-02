using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace string_finder
{
    public partial class Form1 : Form
    {
        private SynchronizationContext syncContext;
        private StringFinder stringFinder;

        public Form1()
        {
            InitializeComponent();
            initListView();

            syncContext = SynchronizationContext.Current;

            choose_folder_button.Click += ChooseFolderClick;
            search_button.Click += SearchButtonClick;
            cancel_button.Click += CancelButtonClick;
            SetStyle(ControlStyles.OptimizedDoubleBuffer, true);

            stringFinder = new StringFinder((fileName, idx, line) =>
            {
                var itm = new ListViewItem(new[] { fileName, idx, line });
                syncContext.Post((listItem) => result_list.Items.Add(listItem as ListViewItem), itm);
            });
        }

        private void CancelButtonClick(object sender, EventArgs e)
        {
            stringFinder.CancelSearch();
        }

        private void initListView()
        {
            result_list.Columns.Add("FileName", 350, HorizontalAlignment.Left);
            result_list.Columns.Add("LineNr", 50, HorizontalAlignment.Left);
            result_list.Columns.Add("LineContent", 600, HorizontalAlignment.Left);
        }

        void ChooseFolderClick(object sender, EventArgs e)
        {
            DialogResult result = folderBrowserDialog1.ShowDialog();
            if (result == DialogResult.OK)
                directory_box.Text = folderBrowserDialog1.SelectedPath;
        }

        private async void SearchButtonClick(object sender, EventArgs e)
        {
            String directory = directory_box.Text;
            String textToSearch = string_box.Text;
            if (ValidateInput(directory, textToSearch))
                await PerformSearch(directory, textToSearch);
        }

        private Boolean ValidateInput(String directory, String textToSearch)
        {
            if (!Directory.Exists(directory))
            {
                string caption = "Invalid directory!";
                string message = "The directory provided is not valid, try enter another one.";
                MessageBoxButtons buttons = MessageBoxButtons.OK;
                MessageBox.Show(message, caption, buttons);
                return false;
            }
            else if (textToSearch == "")
            {
                string caption = "Missing text to search!";
                string message = "Enter a text to search in all the files in this directory.";
                MessageBoxButtons buttons = MessageBoxButtons.OK;
                MessageBox.Show(message, caption, buttons);
                return false;
            }

            return true;
        }

        private async Task PerformSearch(String directory, String textToSearch)
        {
            result_list.Items.Clear();
            search_button.Enabled = false;
            choose_folder_button.Enabled = false;
            cancel_button.Enabled = true;

            await Task.Factory.StartNew(async () =>
            {
                try
                {
                    await stringFinder.PerformSearch(directory, textToSearch);
                }
                catch (OperationCanceledException ex)
                {
                    syncContext.Send(arg =>
                    {
                        string caption = "Search canceled!";
                        string message = "Search was canceled before finishing.";
                        MessageBoxButtons buttons = MessageBoxButtons.OK;
                        MessageBox.Show(message, caption, buttons);
                    }, null);
                }
                finally
                {
                    stringFinder.DisposeCTS();
                    syncContext.Send(arg =>
                    {
                        search_button.Enabled = true;
                        choose_folder_button.Enabled = true;
                        cancel_button.Enabled = false;
                    }, null);
                }
            });
        }
    }
}
