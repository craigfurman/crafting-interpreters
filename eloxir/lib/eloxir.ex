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
    scanner = Scanner.new(file)
    for token <- read_tokens(scanner), do: IO.inspect(token)
    File.close(file)
  end

  # TODO remove once we have a parser + interpreter
  defp read_tokens(scanner, tokens \\ []) do
    {token, scanner} = Scanner.next_token(scanner)

    case token do
      :eof -> Enum.reverse(tokens)
      _ -> read_tokens(scanner, [token | tokens])
    end
  end
end
