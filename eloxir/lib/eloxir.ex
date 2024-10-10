defmodule Eloxir do
  def main(args) do
    # TODO command prompt for no args
    case args do
      [arg] ->
        run_file(arg)

      _ ->
        IO.puts(:stderr, "Usage: eloxir <file>")
        System.halt(64)
    end
  end

  defp run_file(path) do
    file = File.open!(path)
    for token <- Scanner.stream(file), do: IO.inspect(token)
    File.close(file)
  end
end
